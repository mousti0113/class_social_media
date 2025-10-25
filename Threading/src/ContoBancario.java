public class ContoBancario {

    private float saldo;
    private int id;

    public ContoBancario(int id) {
        this.saldo = 1000.00f;
        this.id=id;
    }

    public float getSaldo() {
        return saldo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setSaldo(float saldo) {
        this.saldo = saldo;
    }

    public boolean preleva(float somma_da_prelevare){
        if(this.saldo>somma_da_prelevare){
            this.saldo=this.saldo-somma_da_prelevare;
            return true;

        }
        return false;
    }

    public void deposita(float somma_da_depositare){
        this.saldo=this.saldo+somma_da_depositare;
    }

    @Override
    public String toString() {
        return "ContoBancario{" +
                "saldo=" + saldo +
                '}';
    }
}
