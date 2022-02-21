import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.newrelic.parser.Service;
import org.newrelic.parser.model.Unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(OrderAnnotation.class)
public class ServiceTests {

  @Test
  @Order(0)
  public void test0() throws Exception {
    int availableProcessors = Runtime.getRuntime().availableProcessors();
    System.out.println("availableProcessors: " + availableProcessors);

    assertTrue(availableProcessors > 0);
  }

  @Test
  @Order(1)
  public void test1() throws Exception {
    String text1 = String.join(System.lineSeparator(),
        String.join(" ",
            " , aaa bbb ccc"
        ),
        String.join(" ",
            "ddd eee fff DDD EEE FFF"
        ),
        String.join(" ",
            "ggg hhh i GGG HHH I ggg hhh i"
        )
    );
    String text2 = String.join(System.lineSeparator(),
        String.join(" ",
            " , aaa bbb ccc"
        ),
        String.join(" ",
            "ddd eee fff DDD EEE FFF"
        ),
        String.join(" ",
            "ggg hhh i GGG HHH I ggg hhh i"
        ),
        "  ",
        ""
    );
    Service service = new Service();
    List<Entry<Unit, Integer>> result = service.parseTexts(text1, text2);
    result = service.top(3, result);

    result.forEach(System.out::println);

    assertEquals("ggg hhh i=6", result.get(0).toString());
    if (result.get(1).toString().startsWith("i ggg hhh")) {
      assertEquals("i ggg hhh=4", result.get(1).toString());
      assertEquals("hhh i ggg=4", result.get(2).toString());
    } else {
      assertEquals("hhh i ggg=4", result.get(1).toString());
      assertEquals("i ggg hhh=4", result.get(2).toString());
    }
  }

  @Test
  @Order(2)
  public void test2() throws Exception {
    Path path1 = Paths.get("src", "test", "resources", "txt", "1.txt");
    Path path2 = Paths.get("src", "test", "resources", "txt", "2.txt");
    Service service = new Service();
    List<Entry<Unit, Integer>> result = service.parseFiles(
        path1.toFile().getAbsolutePath(),
        path2.toFile().getAbsolutePath()
    );
    result = service.top(3, result);

    result.forEach(System.out::println);

    assertEquals("ggg hhh i=6", result.get(0).toString());
    if (result.get(1).toString().startsWith("i ggg hhh")) {
      assertEquals("i ggg hhh=4", result.get(1).toString());
      assertEquals("hhh i ggg=4", result.get(2).toString());
    } else {
      assertEquals("hhh i ggg=4", result.get(1).toString());
      assertEquals("i ggg hhh=4", result.get(2).toString());
    }
  }

  @Test
  @Order(3)
  public void test3() throws Exception {
    String text = String.join(System.lineSeparator(),
        String.join(" ",
            " , aaa bbb ccc"
        ),
        String.join(" ",
            "ddd eee fff DDD EEE FFF"
        ),
        String.join(" ",
            "ggg hhh i GGG HHH I ggg hhh i"
        )
    );
    Service service = new Service();
    List<Entry<Unit, Integer>> result;
    try (
        InputStream inputStream = new ByteArrayInputStream(text.getBytes())
    ) {
      result = service.parseInputStream(inputStream);
    }
    result = service.top(3, result);

    result.forEach(System.out::println);

    assertEquals("ggg hhh i=3", result.get(0).toString());
    if (result.get(1).toString().startsWith("i ggg hhh")) {
      assertEquals("i ggg hhh=2", result.get(1).toString());
      assertEquals("hhh i ggg=2", result.get(2).toString());
    } else {
      assertEquals("hhh i ggg=2", result.get(1).toString());
      assertEquals("i ggg hhh=2", result.get(2).toString());
    }
  }

}
