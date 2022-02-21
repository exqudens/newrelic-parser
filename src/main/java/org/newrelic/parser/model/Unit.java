package org.newrelic.parser.model;

import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
@EqualsAndHashCode
public class Unit {

  @NonNull
  List<String> words;

  public Unit(List<String> words) {
    if (words.size() != 3) {
      throw new RuntimeException("words.size() != 3");
    }
    this.words = words;
  }

  public Unit(String... words) {
    if (words.length != 3) {
      throw new RuntimeException("words.length != 3");
    }
    this.words = Arrays.asList(words);
  }

  public Unit(String word0, String word1, String word2) {
    this.words = Arrays.asList(word0, word1, word2);
  }

  @Override
  public String toString() {
    return String.join(" ", words);
  }
}
