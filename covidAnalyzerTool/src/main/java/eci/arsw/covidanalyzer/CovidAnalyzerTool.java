package eci.arsw.covidanalyzer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Camel Application
 */
public class CovidAnalyzerTool {

    private static boolean pause;
    private ResultAnalyzer resultAnalyzer;
    private TestReader testReader;
    private int amountOfFilesTotal;
    private AtomicInteger amountOfFilesProcessed;
    private final int numberThread = 5;
    private static ArrayList<CovidThread> threads;

    public CovidAnalyzerTool() {
        amountOfFilesTotal = 0;
        pause = false;
        threads = new ArrayList<>();
        resultAnalyzer = new ResultAnalyzer();
        testReader = new TestReader();
        amountOfFilesProcessed = new AtomicInteger();
    }

    public void processResultData() {
        amountOfFilesProcessed.set(0);
        List<File> resultFiles = getResultFileList();
        amountOfFilesTotal = resultFiles.size();
        int range = amountOfFilesTotal/numberThread;

        for (int i = 0; i < numberThread; i++){
            if( i != numberThread -1){
                threads.add(new CovidThread(resultFiles,resultAnalyzer,amountOfFilesProcessed,testReader, range * i, (range * i)+ range - 1));
            }else {
                threads.add(new CovidThread(resultFiles,resultAnalyzer,amountOfFilesProcessed,testReader, range * i, amountOfFilesTotal - 1));
            }
            threads.get(threads.size()-1).start();
        }
    }

    private List<File> getResultFileList() {
        List<File> csvFiles = new ArrayList<>();
        try (Stream<Path> csvFilePaths = Files.walk(Paths.get("src/main/resources/")).filter(path -> path.getFileName().toString().endsWith(".csv"))) {
            csvFiles = csvFilePaths.map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return csvFiles;
    }


    public Set<Result> getPositivePeople() {
        return resultAnalyzer.listOfPositivePeople();
    }

    /**
     * A main() so we can easily run these routing rules in our IDE
     */
    public static void main(String... args) throws Exception {
        CovidAnalyzerTool covidAnalyzerTool = new CovidAnalyzerTool();
        Thread processingThread = new Thread(() -> covidAnalyzerTool.processResultData());
        processingThread.start();
        while (true) {
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            if (line.contains("exit")) {
                break;
            }else if (line.isEmpty()){
                if(!pause){
                    System.out.println("Hola");
                    pauseThread();
                    String message = "Processed %d out of %d files.\nFound %d positive people:\n%s";
                    Set<Result> positivePeople = covidAnalyzerTool.getPositivePeople();
                    String affectedPeople = positivePeople.stream().map(Result::toString).reduce("", (s1, s2) -> s1 + "\n" + s2);
                    message = String.format(message, covidAnalyzerTool.amountOfFilesProcessed.get(), covidAnalyzerTool.amountOfFilesTotal, positivePeople.size(), affectedPeople);
                    System.out.println(message);
                }else{
                    resumeThread();
                }

            }

        }

    }

    private static void pauseThread() {
        System.out.println("----pauseThread----");
        pause = true;
        for(CovidThread thread: threads){
            thread.stopFunction();
        }
    }

    private static void resumeThread() {
        System.out.println("----resumeThread----");
        pause = false;
        for(CovidThread thread: threads){
            thread.resumeFunction();
        }
    }

}

