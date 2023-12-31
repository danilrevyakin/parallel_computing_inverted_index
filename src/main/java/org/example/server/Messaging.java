package org.example.server;

public enum Messaging {
    OPTIONS("""
            Specify command:
            1. Find files and positions by word/phrase
            2. Check indexing status
            3. See options
            4. Disconnect"""
    ),
    IN_PROCESS("Indexing is in process..."),
    REQUIRE_INDEXING("Index require population. Please specify a number of threads for execution: "),
    EXECUTION_TIME("Indexing execution time: "),
    INDEX_READY("Index is ready!"),
    INDEX_NOT_READY("Index hasn't been populated yet"),
    ENTER_WORD("Please enter a word/phrase"),
    DISCONNECT("Disconnected successfully!");

    private final String title;

    Messaging(String title) {
        this.title = title;
    }

    public String get() {
        return title;
    }
}
