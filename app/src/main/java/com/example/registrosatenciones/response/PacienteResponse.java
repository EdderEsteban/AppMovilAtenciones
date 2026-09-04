package com.example.registrosatenciones.response;

public class PacienteResponse {
    private int id;
    private String apellido;
    private String nombre;
    private String dni;
    private int edad;
    private String sexo;
    private Integer obraSocialId; // nullable
    private String obraSocial;   // nullable, nombre
    private String telefono;    // nullable

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public Integer getObraSocialId() { return obraSocialId; }
    public void setObraSocialId(Integer obraSocialId) { this.obraSocialId = obraSocialId; }

    public String getObraSocial() { return obraSocial; }
    public void setObraSocial(String obraSocial) { this.obraSocial = obraSocial; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}