package OrientadaaObjeto;
 class Data {
    // Atributos
    private final int ano;
    private final int mes;
    private final int dia;

    // Construtor
    public Data(int dia, int mes, int ano) {
        this.dia = dia;
        this.mes = mes;
        this.ano = ano;
    }

    // Método para formatar a data
    public String obterDataFormatada() {
        return String.format("%d/%d/%d", dia, mes, ano);
    }
}