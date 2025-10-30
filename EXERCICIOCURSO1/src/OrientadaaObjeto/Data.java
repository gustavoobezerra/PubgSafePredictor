package OrientadaaObjeto;

/**
 * Representa uma data com dia, mês e ano.
 * Oferece um construtor para inicializar a data e um método para
 * obter a data formatada como uma string.
 */
 class Data {
    /**
     * O ano da data.
     */
    private final int ano;
    /**
     * O mês da data.
     */
    private final int mes;
    /**
     * O dia da data.
     */
    private final int dia;

    /**
     * Construtor da classe Data.
     *
     * @param dia O dia da data.
     * @param mes O mês da data.
     * @param ano O ano da data.
     */
    public Data(int dia, int mes, int ano) {
        this.dia = dia;
        this.mes = mes;
        this.ano = ano;
    }

    /**
     * Retorna a data formatada como "dia/mês/ano".
     *
     * @return Uma string com a data formatada.
     */
    public String obterDataFormatada() {
        return String.format("%d/%d/%d", dia, mes, ano);
    }
}
