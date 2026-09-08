package com.bbva.services;

import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement
public class NotificationDTO {

    @JsonProperty("CANALOR")
    private String canalor;

    @JsonProperty("CONTROR")
    private String contror;

    @JsonProperty("SERVIOR")
    private String servior;

    @JsonProperty("TIPOPER")
    private String tipoper;

    @JsonProperty("NUMCONV")
    private String cnvenio;

    @JsonProperty("COMISON")
    private Double comison;

    @JsonProperty("IVA")
    private Double iva;

    @JsonProperty("BANDERA")
    private String bandera;

    @JsonProperty("NUMETDC")
    private String numetdc;

    @JsonProperty("TIPPROM")
    private String tipprom;

    @JsonProperty("MARCAPR")
    private String marcapr;

    @JsonProperty("TIPOTAR")
    private String tipotar;

    @JsonProperty("PLAZOMA")
    private String plazoma;

    @JsonProperty("REFEREN")
    private String rferenc;

    @JsonProperty("CONCEPT")
    private String cncepto;

    @JsonProperty("IMEFECT")
    private Double imefect;

    @JsonProperty("IMTOTAL")
    private Double imptota;

    @JsonProperty("CTACARG")
    private String ctacarg;

    @JsonProperty("DISPONI")
    private Double disponi;

    @JsonProperty("DIVISA")
    private String moneda;

    @JsonProperty("TERMINA")
    private String trminal;

    @JsonProperty("USUARIO")
    private String usuario;

    @JsonProperty("FOLIOCA")
    private String folioca;

    @JsonProperty("TIPTRAS")
    private String tiptras;

    @JsonProperty("DCOCARG")
    private String dcocarg;

    @JsonProperty("REFERCA")
    private String referca;

    @JsonProperty("AORIGAB")
    private String aorigab;

    @JsonProperty("REFERAB")
    private String referab;

    // Campos adicionales para trazabilidad y respuesta del servicio
    @JsonProperty("GUIACIE")
    private String guiacie;

    @JsonProperty("NUMMENS")
    private Long nummens;

    @JsonProperty("IDMSJPA")
    private Long idmsjpa;

    @JsonProperty("TIMESTA")
    private String timesta;

    @JsonProperty("FILLMAS")
    private String fillmas;

    @JsonProperty("FILESP2")
    private String filesp2;

    @JsonProperty("FILLNUM")
    private String fillnum;

    @JsonProperty("FILESP1")
    private String filesp1;

    @JsonProperty("IDED")
    private String ided;

    @JsonProperty("AUTORIZ")
    private String autoriz;

    @JsonProperty("CODRESP")
    private String codresp;

    @JsonProperty("TRNS")
    private String trns;

    @JsonProperty("FECPAGO")
    private String fecpago;

    @JsonProperty("HORPAGO")
    private String horpago;

    @JsonProperty("CODFIJO")
    private String codfijo;

    @JsonProperty("FECCAP")
    private String feccap;

    @JsonProperty("FLPAGEM")
    private String flpagem;

    @JsonProperty("TPPAGEM")
    private String tppagem;

    @JsonProperty("CLCUREM")
    private String clcurem;

    @JsonProperty("IMMINEM")
    private Double imminem;

    @JsonProperty("IMMAXEM")
    private Double immaxem;

    @JsonProperty("FHVIGEM")
    private String fhvigem;

    @JsonProperty("CERTIEM")
    private String certiem;

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
}