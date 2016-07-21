import static com.google.copybara.testing.LogSubjects.assertThatConsole;
import com.google.copybara.Destination.WriterResult;
    WriterResult destinationResult = destination.newWriter().write(result, console);
    assertThat(destinationResult).isEqualTo(WriterResult.OK);

    assertThatConsole(console)
        .matchesNext(MessageType.PROGRESS, "Git Destination: Fetching file:.*")
        .matchesNext(MessageType.PROGRESS, "Git Destination: Adding files for push")
        .equalsNext(MessageType.INFO, "\n"
        .matchesNext(MessageType.WARNING, "Proceed with push to.*[?]")
        .matchesNext(MessageType.PROGRESS, "Git Destination: Pushing to .*")
        .containsNoMoreMessages();
    WriterResult result = writer.write(TransformResults.of(workdir, new DummyReference("ref1")), console);
    assertThat(result).isEqualTo(WriterResult.OK);
    result = writer.write(TransformResults.of(workdir, new DummyReference("ref2")), console);
    assertThat(result).isEqualTo(WriterResult.OK);