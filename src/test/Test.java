import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Test {
  public static void main(String[] args) {
    String content = null;
    try {
      content = new String(Files.readAllBytes(Paths.get("src/main/resources/test1.json"))).trim();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    JSONObject json = new JSONObject(content);
    Transformer transformer = new Transformer(json);
    transformer.knimeToPplan();
    transformer.save();
  }
}
