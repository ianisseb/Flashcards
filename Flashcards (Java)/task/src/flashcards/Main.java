package flashcards;

import java.io.*;
import java.util.*;

public class Main {
    private static final String MENU =
            "Input the action (add, remove, import, export, ask, exit, log, hardest card, reset stats):";

    private final Scanner sc = new Scanner(System.in);
    private final Map<String, String> listOfFlashCards = new LinkedHashMap<>();
    private final Map<String, Integer> errorCount = new LinkedHashMap<>();
    private final List<String> log = new ArrayList<>();

    private String importPath = null;
    private String exportPath = null;

    public static void main(String[] args) {
        Main game = new Main();
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-import")) {
                game.importPath = args[i + 1];
            } else if (args[i].equals("-export")) {
                game.exportPath = args[i + 1];
            }
        }
        game.run();
    }

    private void printMessage(String message) {
        System.out.println(message);
        log.add(message);
    }

    private String readInput() {
        String input = sc.nextLine();
        log.add(input);
        return input;
    }

    private String askFileName() {
        printMessage("File name:");
        return readInput();
    }

    public void run() {
        if (importPath != null) {
            loadCards(importPath);
        }

        while (true) {
            printMessage(MENU);
            String input = readInput();
            switch (input) {
                case "add" -> add();
                case "remove" -> remove();
                case "import" -> loadCards(askFileName());
                case "export" -> saveCards(askFileName());
                case "ask" -> ask();
                case "log" -> log();
                case "hardest card" -> hardestCard();
                case "reset stats" -> reset();
                case "exit" -> {
                    printMessage("Bye bye!");
                    if (exportPath != null) {
                        saveCards(exportPath);
                    }
                    return;
                }
            }
        }
    }

    protected void add() {
        printMessage("The card:");
        String cardTerm = readInput();

        if (listOfFlashCards.containsKey(cardTerm)) {
            printMessage(String.format("The card \"%s\" already exists.", cardTerm));
            return;
        }

        printMessage("The definition of the card:");
        String cardDefinition = readInput();

        if (listOfFlashCards.containsValue(cardDefinition)) {
            printMessage(String.format("The definition \"%s\" already exists.", cardDefinition));
            return;
        }

        listOfFlashCards.put(cardTerm, cardDefinition);
        errorCount.put(cardTerm, 0);

        printMessage(String.format("The pair (\"%s\":\"%s\") has been added.", cardTerm, cardDefinition));
    }

    private void remove() {
        printMessage("Which card?");
        String input = readInput();

        if (listOfFlashCards.containsKey(input)) {
            listOfFlashCards.remove(input);
            errorCount.remove(input);
            printMessage("The card has been removed.");
        } else {
            printMessage(String.format("Can't remove \"%s\": there is no such card.", input));
        }
    }

    private void loadCards(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            printMessage("File not found.");
            return;
        }

        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String cardTerm;
            while ((cardTerm = br.readLine()) != null) {
                String cardDefinition = br.readLine();
                String numberOfErrors = br.readLine();

                if (cardDefinition != null && numberOfErrors != null) {
                    listOfFlashCards.put(cardTerm, cardDefinition);
                    errorCount.put(cardTerm, Integer.parseInt(numberOfErrors));
                    count++;
                }
            }
            printMessage(String.format("%d cards have been loaded.", count));
        } catch (IOException e) {
            printMessage("File not found.");
        }
    }

    private void saveCards(String fileName) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
            for (Map.Entry<String, String> entry : listOfFlashCards.entrySet()) {
                pw.println(entry.getKey());
                pw.println(entry.getValue());
                pw.println(errorCount.getOrDefault(entry.getKey(), 0));
            }
            printMessage(String.format("%d cards have been saved.", listOfFlashCards.size()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ask() {
        printMessage("How many times to ask?");
        int numberOfCards = Integer.parseInt(readInput());
        int asked = 0;

        while (asked < numberOfCards && !listOfFlashCards.isEmpty()) {
            for (Map.Entry<String, String> entry : listOfFlashCards.entrySet()) {
                if (asked >= numberOfCards) {
                    break;
                }

                printMessage(String.format("Print the definition of \"%s\":", entry.getKey()));
                String inputDefinition = readInput();

                if (inputDefinition.equals(entry.getValue())) {
                    printMessage("Correct!");
                } else if (listOfFlashCards.containsValue(inputDefinition)) {
                    String correctTermForInput = "";
                    for (Map.Entry<String, String> e : listOfFlashCards.entrySet()) {
                        if (e.getValue().equals(inputDefinition)) {
                            correctTermForInput = e.getKey();
                            break;
                        }
                    }
                    printMessage(String.format("Wrong. The right answer is \"%s\", but your definition is correct for \"%s\".",
                            entry.getValue(), correctTermForInput));
                    errorCount.computeIfPresent(entry.getKey(), (key, oldValue) -> oldValue + 1);
                } else {
                    printMessage(String.format("Wrong. The right answer is \"%s\".", entry.getValue()));
                    errorCount.computeIfPresent(entry.getKey(), (key, oldValue) -> oldValue + 1);
                }

                asked++;
            }
        }
    }

    private void log() {
        String fileName = askFileName();

        printMessage("The log has been saved.");

        // The next menu prompt is expected in the saved file too, so add it
        // temporarily; run() will log it for real when it prints it.
        log.add(MENU);

        try (PrintWriter pw = new PrintWriter(new File(fileName))) {
            for (String line : log) {
                pw.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.removeLast();
    }

    private void hardestCard() {
        int maxValue = 0;
        for (int value : errorCount.values()) {
            if (value > maxValue) {
                maxValue = value;
            }
        }

        if (maxValue == 0) {
            printMessage("There are no cards with errors.");
            return;
        }

        List<String> maxKeys = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : errorCount.entrySet()) {
            if (entry.getValue() == maxValue) {
                maxKeys.add(entry.getKey());
            }
        }

        if (maxKeys.size() > 1) {
            List<String> formatted = new ArrayList<>();
            for (String key : maxKeys) {
                formatted.add("\"" + key + "\"");
            }
            printMessage(String.format("The hardest cards are %s. You have %d errors answering them.",
                    String.join(", ", formatted), maxValue));
        } else {
            printMessage(String.format("The hardest card is \"%s\". You have %d errors answering it.",
                    maxKeys.getFirst(), maxValue));
        }
    }

    private void reset() {
        errorCount.replaceAll((key, value) -> 0);
        printMessage("Card statistics have been reset.");
    }
}