import com.google.copybara.Author;
import com.google.copybara.Destination;
import com.google.copybara.testing.TransformResults;
import com.google.copybara.util.console.testing.TestingConsole;
import com.google.copybara.util.console.testing.TestingConsole.MessageType;
  private TestingConsole console;
    console = new TestingConsole();
    options = new OptionsBuilder().setConsole(console);
    return destinationFirstCommit(/*askConfirmation*/ false);
  }

  private GitDestination destinationFirstCommit(boolean askConfirmation)
      throws ConfigValidationException {
    return yaml.withOptions(options.build(), CONFIG_NAME, askConfirmation);
    return yaml.withOptions(options.build(), CONFIG_NAME, /*askConfirmation*/ false);
  private void assertCommitHasAuthor(String branch, Author author) throws RepoException {
    assertThat(git("--git-dir", repoGitDir.toString(), "log", "-n1",
        "--pretty=format:\"%an <%ae>\"", branch))
        .isEqualTo("\"" + author + "\"");
  }

      throws RepoException, ConfigValidationException, IOException {
    TransformResult result = TransformResults.of(workdir, originRef, excludedDestinationPaths);
    if (baseline != null) {
      result = result.withBaseline(baseline);
    }
    destination.newWriter().write(result, console);
  @Test
  public void processUserAborts() throws Exception {
    console = new TestingConsole()
        .respondNo();
    yaml.setFetch("master");
    yaml.setPush("master");
    Files.write(workdir.resolve("test.txt"), "some content".getBytes());
    thrown.expect(RepoException.class);
    thrown.expectMessage("User aborted execution: did not confirm diff changes");
    process(destinationFirstCommit(/*askConfirmation*/ true), new DummyReference("origin_ref"));
  }

  @Test
  public void processUserConfirms() throws Exception {
    console = new TestingConsole()
        .respondYes();
    yaml.setFetch("master");
    yaml.setPush("master");
    Files.write(workdir.resolve("test.txt"), "some content".getBytes());
    process(destinationFirstCommit(/*askConfirmation*/ true), new DummyReference("origin_ref"));
    console
        .assertNextMatches(MessageType.PROGRESS, "Git Destination: Fetching file:.*")
        .assertNextMatches(MessageType.PROGRESS, "Git Destination: Adding files for push")
        .assertNextEquals(MessageType.INFO, "\n"
            + "diff --git a/test.txt b/test.txt\n"
            + "new file mode 100644\n"
            + "index 0000000..f0eec86\n"
            + "--- /dev/null\n"
            + "+++ b/test.txt\n"
            + "@@ -0,0 +1 @@\n"
            + "+some content\n"
            + "\\ No newline at end of file\n")
        .assertNextMatches(MessageType.WARNING, "Proceed with push to.*[?]")
        .assertNextMatches(MessageType.PROGRESS, "Git Destination: Pushing to .*")
        .assertNoMore();
  }

  @Test
  public void authorPropagated() throws Exception {
    yaml.setFetch("master");
    yaml.setPush("master");

    Files.write(workdir.resolve("test.txt"), "some content".getBytes());

    DummyReference firstCommit = new DummyReference("first_commit")
        .withAuthor(new Author("Foo Bar", "foo@bar.com"))
        .withTimestamp(1414141414);
    process(destinationFirstCommit(), firstCommit);

    assertCommitHasAuthor("master", new Author("Foo Bar", "foo@bar.com"));
  }


  @Test
  public void pushSequenceOfChangesToReviewBranch() throws Exception {
    yaml.setFetch("master");
    yaml.setPush("refs_for_master");

    Destination.Writer writer = destinationFirstCommit().newWriter();

    Files.write(workdir.resolve("test42"), "42".getBytes(UTF_8));
    writer.write(TransformResults.of(workdir, new DummyReference("ref1")), console);
    String firstCommitHash = repo().simpleCommand("rev-parse", "refs_for_master").getStdout();

    Files.write(workdir.resolve("test99"), "99".getBytes(UTF_8));
    writer.write(TransformResults.of(workdir, new DummyReference("ref2")), console);

    // Make sure parent of second commit is the first commit.
    assertThat(repo().simpleCommand("rev-parse", "refs_for_master~1").getStdout())
        .isEqualTo(firstCommitHash);

    // Make sure commits have correct file content.
    GitTesting.assertThatCheckout(repo(), "refs_for_master~1")
        .containsFile("test42", "42")
        .containsNoMoreFiles();
    GitTesting.assertThatCheckout(repo(), "refs_for_master")
        .containsFile("test42", "42")
        .containsFile("test99", "99")
        .containsNoMoreFiles();
  }