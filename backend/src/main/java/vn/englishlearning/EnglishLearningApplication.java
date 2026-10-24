package vn.englishlearning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import vn.englishlearning.contentops.cli.CurriculumCli;

@SpringBootApplication
public class EnglishLearningApplication {

  public static void main(String[] args) {
    if (args.length > 0 && "content".equalsIgnoreCase(args[0])) {
      int exitCode = CurriculumCli.execute(args);
      if (exitCode != 0) {
        System.exit(exitCode);
      }
      return;
    }
    SpringApplication.run(EnglishLearningApplication.class, args);
  }
}
