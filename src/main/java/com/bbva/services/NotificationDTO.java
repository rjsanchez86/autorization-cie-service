package com.bbva.services;

import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationDTO {

    @JsonProperty("canalor")
    private String canalor;

    @JsonProperty("contror")
    private String contror;

    @JsonProperty("servior")
    private String servior;

    @JsonProperty("tipoper")
    private String tipoper;

    @JsonProperty("cnvenio")
    private String cnvenio;

    @JsonProperty("comison")
    private Double comison;

    @JsonProperty("iva")
    private Double iva;

    @JsonProperty("bandera")
    private String bandera;

    @JsonProperty("numetdc")
    private String numetdc;

    @JsonProperty("tipprom")
    private String tipprom;

    @JsonProperty("marcapr")
    private String marcapr;

    @JsonProperty("tipotar")
    private String tipotar;

    @JsonProperty("plazoma")
    private String plazoma;

    @JsonProperty("rferenc")
    private String rferenc;

    @JsonProperty("cncepto")
    private String cncepto;

    @JsonProperty("imefect")
    private Double imefect;

    @JsonProperty("imptota")
    private Double imptota;

    @JsonProperty("ctacarg")
    private String ctacarg;

    @JsonProperty("disponi")
    private Double disponi;

    @JsonProperty("moneda")
    private String moneda;

    @JsonProperty("trminal")
    private String trminal;

    @JsonProperty("usuario")
    private String usuario;

    @JsonProperty("folioca")
    private String folioca;

    @JsonProperty("tiptras")
    private String tiptras;

    @JsonProperty("dcocarg")
    private String dcocarg;

    @JsonProperty("referca")
    private String referca;

    @JsonProperty("aorigab")
    private String aorigab;

    @JsonProperty("referab")
    private String referab;

    // Campos adicionales para trazabilidad y respuesta del servicio
    @JsonProperty("guiacie")
    private String guiacie;

    @JsonProperty("nummens")
    private Long nummens;

    @JsonProperty("idmsjpa")
    private Long idmsjpa;

    @JsonProperty("timesta")
    private String timesta;

    @JsonProperty("fillmas")
    private String fillmas;

    @JsonProperty("filesp2")
    private String filesp2;

    @JsonProperty("fillnum")
    private String fillnum;

    @JsonProperty("filesp1")
    private String filesp1;

    @JsonProperty("ided")
    private String ided;

    @JsonProperty("autoriz")
    private String autoriz;

    @JsonProperty("codresp")
    private String codresp;

    @JsonProperty("trns")
    private String trns;

    @JsonProperty("fecpago")
    private String fecpago;

    @JsonProperty("horpago")
    private String horpago;

    @JsonProperty("codfijo")
    private String codfijo;

    @JsonProperty("feccap")
    private String feccap;

    @JsonProperty("flpagem")
    private String flpagem;

    @JsonProperty("tppagem")
    private String tppagem;

    @JsonProperty("clcurem")
    private String clcurem;

    @JsonProperty("imminem")
    private Double imminem;

    @JsonProperty("immaxem")
    private Double immaxem;

    @JsonProperty("fhvigem")
    private String fhvigem;

    @JsonProperty("certiem")
    private String certiem;

    @JsonIgnore
    private Integer code;

    @JsonIgnore
    public Integer getCode() { return code; }
    @JsonProperty("code")
    public void setCode(Integer code) { this.code = code; }

    public NotificationDTO() {}

    // Getters y Setters
    public String getCanalor() { return canalor; }
    public void setCanalor(String canalor) { this.canalor = canalor; }
    public String getContror() { return contror; }
    public void setContror(String contror) { this.contror = contror; }
    public String getServior() { return servior; }
    public void setServior(String servior) { this.servior = servior; }
    public String getTipoper() { return tipoper; }
    public void setTipoper(String tipoper) { this.tipoper = tipoper; }
    public Double getImptota() { return imptota; }
    public void setImptota(Double imptota) { this.imptota = imptota; }
    public String getGuiacie() { return guiacie; }
    public void setGuiacie(String guiacie) { this.guiacie = guiacie; }
    public Long getNummens() { return nummens; }
    public void setNummens(Long nummens) { this.nummens = nummens; }
    public Long getIdmsjpa() { return idmsjpa; }
    public void setIdmsjpa(Long idmsjpa) { this.idmsjpa = idmsjpa; }
    public String getTimesta() { return timesta; }
    public void setTimesta(String timesta) { this.timesta = timesta; }
    public String getTrminal() { return trminal; }
    public void setTrminal(String trminal) { this.trminal = trminal; }
    public String getCncepto() { return cncepto; }
    public void setCncepto(String cncepto) { this.cncepto = cncepto; }
    public String getRferenc() { return rferenc; }
    public void setRferenc(String rferenc) { this.rferenc = rferenc; }
    public String getFillmas() { return fillmas; }
    public void setFillmas(String fillmas) { this.fillmas = fillmas; }
    public String getFilesp2() { return filesp2; }
    public void setFilesp2(String filesp2) { this.filesp2 = filesp2; }
    public String getFillnum() { return fillnum; }
    public void setFillnum(String fillnum) { this.fillnum = fillnum; }
    public String getCnvenio() { return cnvenio; }
    public void setCnvenio(String cnvenio) { this.cnvenio = cnvenio; }
    public String getFilesp1() { return filesp1; }
    public void setFilesp1(String filesp1) { this.filesp1 = filesp1; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public String getIded() { return ided; }
    public void setIded(String ided) { this.ided = ided; }
    public String getAutoriz() { return autoriz; }
    public void setAutoriz(String autoriz) { this.autoriz = autoriz; }
    public String getCodresp() { return codresp; }
    public void setCodresp(String codresp) { this.codresp = codresp; }
    public String getTrns() { return trns; }
    public void setTrns(String trns) { this.trns = trns; }
    public String getFecpago() { return fecpago; }
    public void setFecpago(String fecpago) { this.fecpago = fecpago; }
    public String getHorpago() { return horpago; }
    public void setHorpago(String horpago) { this.horpago = horpago; }
    public String getCodfijo() { return codfijo; }
    public void setCodfijo(String codfijo) { this.codfijo = codfijo; }
    public String getFeccap() { return feccap; }
    public void setFeccap(String feccap) { this.feccap = feccap; }
    public String getFlpagem() { return flpagem; }
    public void setFlpagem(String flpagem) { this.flpagem = flpagem; }
    public String getTppagem() { return tppagem; }
    public void setTppagem(String tppagem) { this.tppagem = tppagem; }
    public String getClcurem() { return clcurem; }
    public void setClcurem(String clcurem) { this.clcurem = clcurem; }
    public Double getImminem() { return imminem; }
    public void setImminem(Double imminem) { this.imminem = imminem; }
    public Double getImmaxem() { return immaxem; }
    public void setImmaxem(Double immaxem) { this.immaxem = immaxem; }
    public String getFhvigem() { return fhvigem; }
    public void setFhvigem(String fhvigem) { this.fhvigem = fhvigem; }
    public String getCertiem() { return certiem; }
    public void setCertiem(String certiem) { this.certiem = certiem; }
    public Double getComison() { return comison; }
    public void setComison(Double comison) { this.comison = comison; }
    public Double getIva() { return iva; }
    public void setIva(Double iva) { this.iva = iva; }
    public String getBandera() { return bandera; }
    public void setBandera(String bandera) { this.bandera = bandera; }
    public String getNumetdc() { return numetdc; }
    public void setNumetdc(String numetdc) { this.numetdc = numetdc; }
    public String getTipprom() { return tipprom; }
    public void setTipprom(String tipprom) { this.tipprom = tipprom; }
    public String getMarcapr() { return marcapr; }
    public void setMarcapr(String marcapr) { this.marcapr = marcapr; }
    public String getTipotar() { return tipotar; }
    public void setTipotar(String tipotar) { this.tipotar = tipotar; }
    public String getPlazoma() { return plazoma; }
    public void setPlazoma(String plazoma) { this.plazoma = plazoma; }
    public Double getImefect() { return imefect; }
    public void setImefect(Double imefect) { this.imefect = imefect; }
    public String getCtacarg() { return ctacarg; }
    public void setCtacarg(String ctacarg) { this.ctacarg = ctacarg; }
    public Double getDisponi() { return disponi; }
    public void setDisponi(Double disponi) { this.disponi = disponi; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getFolioca() { return folioca; }
    public void setFolioca(String folioca) { this.folioca = folioca; }
    public String getTiptras() { return tiptras; }
    public void setTiptras(String tiptras) { this.tiptras = tiptras; }
    public String getDcocarg() { return dcocarg; }
    public void setDcocarg(String dcocarg) { this.dcocarg = dcocarg; }
    public String getReferca() { return referca; }
    public void setReferca(String referca) { this.referca = referca; }
    public String getAorigab() { return aorigab; }
    public void setAorigab(String aorigab) { this.aorigab = aorigab; }
    public String getReferab() { return referab; }
    public void setReferab(String referab) { this.referab = referab; }

    /** Normaliza solo la salida JSON del simulador. No altera los valores de entrada antes de validarlos. */
    public void normalizeForResponse() {
        if (this.canalor == null) this.canalor = "";
        if (this.contror == null) this.contror = "";
        if (this.servior == null) this.servior = "";
        if (this.tipoper == null) this.tipoper = "";
        if (this.cnvenio == null) this.cnvenio = "";
        if (this.comison == null) this.comison = 0.0;
        if (this.iva == null) this.iva = 0.0;
        if (this.bandera == null) this.bandera = "";
        if (this.numetdc == null) this.numetdc = "";
        if (this.tipprom == null) this.tipprom = "";
        if (this.marcapr == null) this.marcapr = "";
        if (this.tipotar == null) this.tipotar = "";
        if (this.plazoma == null) this.plazoma = "";
        if (this.rferenc == null) this.rferenc = "";
        if (this.cncepto == null) this.cncepto = "";
        if (this.imefect == null) this.imefect = 0.0;
        if (this.imptota == null) this.imptota = 0.0;
        if (this.ctacarg == null) this.ctacarg = "";
        if (this.disponi == null) this.disponi = 0.0;
        if (this.moneda == null) this.moneda = "";
        if (this.trminal == null) this.trminal = "";
        if (this.usuario == null) this.usuario = "";
        if (this.folioca == null) this.folioca = "";
        if (this.tiptras == null) this.tiptras = "";
        if (this.dcocarg == null) this.dcocarg = "";
        if (this.referca == null) this.referca = "";
        if (this.aorigab == null) this.aorigab = "";
        if (this.referab == null) this.referab = "";
        if (this.guiacie == null) this.guiacie = "";
        if (this.nummens == null) this.nummens = 0L;
        if (this.idmsjpa == null) this.idmsjpa = 0L;
        if (this.timesta == null) this.timesta = "";
        if (this.fillmas == null) this.fillmas = "";
        if (this.filesp2 == null) this.filesp2 = "";
        if (this.fillnum == null) this.fillnum = "";
        if (this.filesp1 == null) this.filesp1 = "";
        if (this.ided == null) this.ided = "";
        if (this.autoriz == null) this.autoriz = "";
        if (this.codresp == null) this.codresp = "";
        if (this.trns == null) this.trns = "";
        if (this.fecpago == null) this.fecpago = "";
        if (this.horpago == null) this.horpago = "";
        if (this.codfijo == null) this.codfijo = "";
        if (this.feccap == null) this.feccap = "";
        if (this.flpagem == null) this.flpagem = "";
        if (this.tppagem == null) this.tppagem = "";
        if (this.clcurem == null) this.clcurem = "";
        if (this.imminem == null) this.imminem = 0.0;
        if (this.immaxem == null) this.immaxem = 0.0;
        if (this.fhvigem == null) this.fhvigem = "";
        if (this.certiem == null) this.certiem = "";
    }
}