package com.printwithai.printer;

import com.printwithai.entity.PrintJob;

public interface PrinterService {

    void submitJob(PrintJob job);

    void retryJob(PrintJob job);

    void cancelJob(PrintJob job);

    String getPrinterStatus();

    boolean isReady();
}
