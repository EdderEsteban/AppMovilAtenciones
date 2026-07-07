package com.example.registrosatenciones.response;

import java.util.List;

public class PacienteDetalleResponse {
    private int id;
    private String apellido;
    private String nombre;
    private String dni;
    private String fechaNacimiento;
    private int edad;
    private String sexo;
    private String domicilio;   // nullable
    private String telefono;    // nullable
    private String obraSocial;  // nullable
    private List<AtencionResumenResponse> atenciones;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getDomicilio() { return domicilio; }
    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getObraSocial() { return obraSocial; }
    public void setObraSocial(String obraSocial) { this.obraSocial = obraSocial; }

    public List<AtencionResumenResponse> getAtenciones() { return atenciones; }
    public void setAtenciones(List<AtencionResumenResponse> atenciones) { this.atenciones = atenciones; }
}