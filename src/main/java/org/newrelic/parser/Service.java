package org.newrelic.parser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.newrelic.parser.model.Unit;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Service {

  Map<Unit, AtomicInteger> entries;
  ExecutorService executor;
  long timeOutMinutes;
  int topCount;

  public Service() {
    entries = new ConcurrentHashMap<>();
    int nThreads = Runtime.getRuntime().availableProcessors() * 2;
    executor = new ThreadPoolExecutor(
        nThreads,
        nThreads,
        0L,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(nThreads * 2),
        Executors.defaultThreadFactory(),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );
    timeOutMinutes = 10L;
    topCount = 100;
  }

  public void run(String... argv) {
    try {
      List<Entry<Unit, Integer>> result;
      if (argv.length != 0) {
        result = parseFiles(argv);
      } else {
        result = parseInputStream(System.in);
      }
      result = top(topCount, result);
      for (Entry<Unit, Integer> entry : result) {
        System.out.println(entry.toString());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<Entry<Unit, Integer>> top(int count, List<Entry<Unit, Integer>> all) {
    if (all.isEmpty()) {
      return all;
    }
    int min = Math.max(all.size() - count, 0);
    int max = all.size() - 1;
    List<Entry<Unit, Integer>> result = new ArrayList<>();
    for (int i = max; i >= min; i--) {
      result.add(all.get(i));
    }
    return result;
  }

  public List<Entry<Unit, Integer>> parseFiles(String... paths) {
    try {
      for (String path : paths) {
        CompletableFuture
            .completedFuture(path)
            .thenAcceptAsync(this::parseFile, executor);

      }
      executor.shutdown();
      if (!executor.awaitTermination(timeOutMinutes, TimeUnit.MINUTES)) {
        throw new Exception("!executor.awaitTermination(" + timeOutMinutes + ", TimeUnit.MINUTES)");
      }
      return sorted();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<Entry<Unit, Integer>> parseTexts(String... texts) {
    try {
      for (String text : texts) {
        CompletableFuture.completedFuture(text)
            .thenAcceptAsync(this::parseText, executor);
      }
      executor.shutdown();
      if (!executor.awaitTermination(timeOutMinutes, TimeUnit.MINUTES)) {
        throw new Exception("!executor.awaitTermination(" + timeOutMinutes + ", TimeUnit.MINUTES)");
      }
      return sorted();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<Entry<Unit, Integer>> parseInputStream(InputStream inputStream) {
    parse(inputStream);
    return sorted();
  }

  private void parseFile(String filePath) {
    try (InputStream inputStream = new FileInputStream(filePath)) {
      parse(inputStream);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void parseText(String text) {
    try (InputStream inputStream = new ByteArrayInputStream(text.getBytes())) {
      parse(inputStream);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void parse(InputStream inputStream) {
    try {
      List<StringBuilder> stringBuilders = new ArrayList<>();
      try (
          InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
          Reader reader = new BufferedReader(inputStreamReader)
      ) {
        boolean previousIsLetter = false;
        int position;
        while ((position = reader.read()) != -1) {
          char ch = (char) position;

          if (!previousIsLetter && Character.isLetter(ch) && stringBuilders.isEmpty()) {
            stringBuilders.add(new StringBuilder());
          }

          if (Character.isLetter(ch)) {
            StringBuilder stringBuilder = stringBuilders.get(stringBuilders.size() - 1);
            stringBuilder.append(ch);

            previousIsLetter = true;
          }

          if (!Character.isLetter(ch)) {
            if (previousIsLetter && stringBuilders.size() < 3) {
              stringBuilders.add(new StringBuilder());
            } else if (previousIsLetter && stringBuilders.size() == 3) {
              Unit unit = createUnit(stringBuilders);
              if (unit == null) {
                throw new Exception("unit == null");
              }
              entries.putIfAbsent(unit, new AtomicInteger());
              entries.get(unit).incrementAndGet();
              stringBuilders.remove(0);
              stringBuilders.add(new StringBuilder());
            }

            previousIsLetter = false;
          }
        }

        if (stringBuilders.size() == 3) {
          Unit unit = createUnit(stringBuilders);
          if (unit != null) {
            entries.putIfAbsent(unit, new AtomicInteger());
            entries.get(unit).incrementAndGet();
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Unit createUnit(List<StringBuilder> stringBuilders) {
    if (stringBuilders.size() > 3) {
      throw new RuntimeException("stringBuilders.size() > 3");
    }

    if (stringBuilders.size() < 3) {
      throw new RuntimeException("stringBuilders.size() < 3");
    }

    List<String> words = new ArrayList<>();

    for (StringBuilder sb : stringBuilders) {
      String word = sb.toString();
      if (!word.isEmpty()) {
        words.add(word.toLowerCase());
      }
    }

    if (words.size() == 3) {
      return new Unit(words);
    } else {
      return null;
    }
  }

  private List<Entry<Unit, Integer>> sorted() {
    List<Entry<Unit, Integer>> sorted = new ArrayList<>();
    for (Entry<Unit, AtomicInteger> entry : entries.entrySet()) {
      Entry<Unit, Integer> object = new SimpleEntry<>(entry.getKey(), entry.getValue().get());
      int search = Collections.binarySearch(sorted, object, Entry.comparingByValue());
      int index = search >= 0 ? search : Math.abs(search) - 1;
      sorted.add(index, object);
    }
    return sorted;
  }

}
