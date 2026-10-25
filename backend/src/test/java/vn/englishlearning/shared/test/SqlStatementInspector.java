package vn.englishlearning.shared.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.resource.jdbc.spi.StatementInspector;

public class SqlStatementInspector implements StatementInspector {

  private static final List<String> QUERIES = Collections.synchronizedList(new ArrayList<>());
  private static volatile boolean recording = false;

  public static void startRecording() {
    QUERIES.clear();
    recording = true;
  }

  public static List<String> stopRecording() {
    recording = false;
    return new ArrayList<>(QUERIES);
  }

  public static List<String> getQueries() {
    return new ArrayList<>(QUERIES);
  }

  @Override
  public String inspect(String sql) {
    if (recording && sql != null) {
      QUERIES.add(sql);
    }
    return sql;
  }
}
