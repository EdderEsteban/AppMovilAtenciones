package com.example.registrosatenciones.request;

public class CrearPacienteRequest {
    private String dni;
    private String apellido;
    private String nombre;
    private String fechaNacimiento;
    private String sexo;
    private String domicilio;
    private String telefono;
    private Integer obraSocialId;

    public CrearPacienteRequest(String dni, String apellido, String nombre, String fechaNacimiento,
                                 String sexo, String domicilio, String telefono, Integer obraSocialId) {
        this.dni = dni;
        this.apellido = apellido;
        this.nombre = nombre;
        this.fechaNacimiento = fechaNacimiento;
        this.sexo = sexo;
        this.domicilio = domicilio;
        this.telefono = telefono;
        this.obraSocialId = obraSocialId;
    }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getDomicilio() { return domicilio; }
    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public Integer getObraSocialId() { return obraSocialId; }
    public void setObraSocialId(Integer obraSocialId) { this.obraSocialId = obraSocialId; }
}
