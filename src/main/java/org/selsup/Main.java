package org.selsup;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.MINUTES, 3);
        api.sendDocument(new CrptApi.Document(), "1");
        api.sendDocument(new CrptApi.Document(), "2");
        api.sendDocument(new CrptApi.Document(), "3");
        api.sendDocument(new CrptApi.Document(), "4");
        api.sendDocument(new CrptApi.Document(), "5");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Спустя 130 секунд добавляем еще реквесты");
            api.sendDocument(new CrptApi.Document(), "6");
            api.sendDocument(new CrptApi.Document(), "7");
            api.sendDocument(new CrptApi.Document(), "8");
            api.sendDocument(new CrptApi.Document(), "9");
            scheduler.shutdownNow();
        }, 130, 1, TimeUnit.SECONDS);
    }
}