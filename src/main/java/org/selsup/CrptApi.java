package org.selsup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final int requestLimit;
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private final BlockingQueue<Callable<String>> taskQueue;

    private final ExecutorService taskExecutor;
    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        ScheduledExecutorService resetLimitExecutor = Executors.newScheduledThreadPool(1);
        this.taskQueue = new LinkedBlockingQueue<>();
        this.taskExecutor = Executors.newCachedThreadPool();
        this.semaphore = new Semaphore(requestLimit);

        limitUpdater(timeUnit, resetLimitExecutor);
        taskQueueListener();
    }



    public void sendDocument(Document document, String signature) {
        Task task = new Task(document, signature);
        taskQueue.add(task);
    }



    private void limitUpdater(TimeUnit timeUnit, ScheduledExecutorService scheduler) {
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Сброс счетчика в 0");
            requestCounter.set(0);
        }, 0, 1, timeUnit);
    }



    private void taskQueueListener() {
        Thread taskProcessor = new Thread(() -> {
            while (true) {
                if (requestCounter.get() < requestLimit) {
                    try {
                        Callable<String> task = taskQueue.take();
                        requestCounter.incrementAndGet();
                        semaphore.acquire();
                        taskExecutor.submit(() -> {
                            try {
                                task.call();
                            } catch (Exception e) {
                                System.out.println("Exception during call the request: ".concat(e.getMessage()));
                            } finally {
                                semaphore.release();
                            }
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
        taskProcessor.setDaemon(true);
        taskProcessor.start();
    }



    private record Task(Document document, String signature) implements Callable<String> {
        @Override
        public String call() throws Exception {
            return postRequest(document, signature);
        }
    }



    private static String postRequest(Document document, String signature) {
        //todo Не понятно что делать с подписью ??? Пока просто вывод в консоль
        System.out.println(signature);

        String requestBody = convertToString(document);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String result = String.format("Status code: %s %nResponse body: %s", response.statusCode(), response.body());
            System.out.println(result);
            return result;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Exception by request: ".concat(e.getMessage());
        }
    }



    private static String convertToString(Document document) {
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = "";
        try {
            requestBody = objectMapper.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return requestBody;
    }


    public static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;

        public static class Description {
            private String participantInn;

            public String getParticipantInn() {
                return participantInn;
            }

            public void setParticipantInn(String participantInn) {
                this.participantInn = participantInn;
            }
        }

        public static class Product {
            private String certificate_document;
            private String certificate_document_date;
            private String certificate_document_number;
            private String owner_inn;
            private String producer_inn;
            private String production_date;
            private String tnved_code;
            private String uit_code;
            private String uitu_code;

            public String getCertificateDocument() {
                return certificate_document;
            }

            public void setCertificateDocument(String certificate_document) {
                this.certificate_document = certificate_document;
            }

            public String getCertificateDocumentDate() {
                return certificate_document_date;
            }

            public void setCertificateDocumentDate(String certificate_document_date) {
                this.certificate_document_date = certificate_document_date;
            }

            public String getCertificateDocumentNumber() {
                return certificate_document_number;
            }

            public void setCertificateDocumentNumber(String certificate_document_number) {
                this.certificate_document_number = certificate_document_number;
            }

            public String getOwnerInn() {
                return owner_inn;
            }

            public void setOwnerInn(String owner_inn) {
                this.owner_inn = owner_inn;
            }

            public String getProducerInn() {
                return producer_inn;
            }

            public void setProducerInn(String producer_inn) {
                this.producer_inn = producer_inn;
            }

            public String getProductionDate() {
                return production_date;
            }

            public void setProductionDate(String production_date) {
                this.production_date = production_date;
            }

            public String getTnvedCode() {
                return tnved_code;
            }

            public void setTnvedCode(String tnved_code) {
                this.tnved_code = tnved_code;
            }

            public String getUitCode() {
                return uit_code;
            }

            public void setUitCode(String uit_code) {
                this.uit_code = uit_code;
            }

            public String getUituCode() {
                return uitu_code;
            }

            public void setUituCode(String uitu_code) {
                this.uitu_code = uitu_code;
            }
        }


        public Description getDescription() {
            return description;
        }

        public void setDescription(Description description) {
            this.description = description;
        }

        public String getDocId() {
            return doc_id;
        }

        public void setDocId(String doc_id) {
            this.doc_id = doc_id;
        }

        public String getDocStatus() {
            return doc_status;
        }

        public void setDocStatus(String doc_status) {
            this.doc_status = doc_status;
        }

        public String getDocType() {
            return doc_type;
        }

        public void setDocType(String doc_type) {
            this.doc_type = doc_type;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwnerInn() {
            return owner_inn;
        }

        public void setOwnerInn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getParticipantInn() {
            return participant_inn;
        }

        public void setParticipantInn(String participant_inn) {
            this.participant_inn = participant_inn;
        }

        public String getProducerInn() {
            return producer_inn;
        }

        public void setProducerInn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public String getProductionDate() {
            return production_date;
        }

        public void setProductionDate(String production_date) {
            this.production_date = production_date;
        }

        public String getProductionType() {
            return production_type;
        }

        public void setProductionType(String production_type) {
            this.production_type = production_type;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }

        public String getRegDate() {
            return reg_date;
        }

        public void setRegDate(String reg_date) {
            this.reg_date = reg_date;
        }

        public String getRegNumber() {
            return reg_number;
        }

        public void setRegNumber(String reg_number) {
            this.reg_number = reg_number;
        }
    }
}
