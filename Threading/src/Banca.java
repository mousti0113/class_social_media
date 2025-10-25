
import java.util.ArrayList;
import java.util.List;

public class Banca {

    private ContoBancario [] conti;

    public Banca() {
    }

    public ContoBancario[] getConti() {
        return conti;
    }

    public void setConti(ContoBancario[] conti) {
        this.conti = conti;
    }
    public boolean fa_bonifico(ContoBancario[] conti, int id_conto_sorge,int id_conto_dest,float somma_bonifico){

        if(conti[id_conto_sorge].getSaldo()>somma_bonifico){
            conti[id_conto_sorge].setSaldo(conti[id_conto_sorge].getSaldo()-somma_bonifico);

            conti[id_conto_dest].setSaldo(  conti[id_conto_dest].getSaldo()+somma_bonifico);
            return true;

        }
        return false;
    }

    public void stampaSaldi(ContoBancario[] conti){
        for (int i = 0; i < conti.length; i++) {
            System.out.println(conti[i].getSaldo());
            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++");
        }
    }

}
