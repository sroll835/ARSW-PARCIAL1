package eci.arsw.covidanalyzer;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CovidThread extends Thread {

    private List<File> resultFile;
    private ResultAnalyzer resultAnalyzer;
    private AtomicInteger amount;
    private TestReader testReader;
    private boolean stop;
    private int min;
    private int max;

    public CovidThread(List<File> resultFile, ResultAnalyzer resultAnalyzer, AtomicInteger amount, TestReader testReader, int min, int max){
        this.resultFile = resultFile;
        this.resultAnalyzer = resultAnalyzer;
        this.amount = amount;
        this.testReader = testReader;
        this.min = min;
        this.max = max;

    }

    public void stopFunction(){
        stop = true;
    }

    public void resumeFunction(){
        stop = false;
        synchronized (this) {
            notifyAll();
        }
    }
    public void run() {
        for (int i = min; i <= max; i++) {
            //System.out.println(i);
            synchronized (this) {
                while (stop) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            List<Result> results = testReader.readResultsFromFile(resultFile.get(i));
            for (Result result : results) {
                resultAnalyzer.addResult(result);
            }
            amount.incrementAndGet();
        }
    }

}
