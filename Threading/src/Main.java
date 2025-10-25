//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
void main() {//TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
    // to see how IntelliJ IDEA suggests fixing it.
ContoBancario [] conti = {new ContoBancario(1),new ContoBancario(2),new ContoBancario(3)};
Banca banca=new Banca();


    try (ExecutorService executorService = Executors.newFixedThreadPool(5)) {

        // Sottomettere task DENTRO il blocco try
        executorService.submit(() -> {
            for (int i = 0; i < 100; i++) {
               banca.fa_bonifico(conti,1,2,5);
            }
        });

        // Fermare l'accettazione di nuovi task
        executorService.shutdown();

        // Attendere che tutti i task finiscano (max 1 minuto)
        if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }




    } catch (Exception e) {
        e.printStackTrace();
    }
    banca.stampaSaldi(conti);
}
