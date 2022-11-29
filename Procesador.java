import java.lang.Exception;
import java.net.Socket;
import java.io.*;
import java.net.*;
import java.util.GregorianCalendar;
import java.util.Calendar;

public class Procesador {

	private static Socket GateProc;

    public static String leeSocket (Socket p_sk, String p_Datos)
	{
		try
		{
			InputStream aux = p_sk.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(aux));
			String cad = br.readLine();
			p_Datos=cad;
		}
		catch (Exception e)
		{
			System.out.println("Error: " + e.toString());
		}
      return p_Datos;
	  
	}

	public static void escribeSocket (Socket p_sk, String p_Datos)
	{
		try
		{
			OutputStream aux = p_sk.getOutputStream();
			DataOutputStream flujo= new DataOutputStream( aux );
			flujo.writeBytes(p_Datos);      
		}
		catch (Exception e)
		{
			System.out.println("Error: " + e.toString());
		}
		return;
	}

	public static int calculaLRC(String cad){

		String LRC = cad;
		int suma = 0;
		for(int i=0; i<LRC.length();i++) {
												
			if (i == 0) {								
				suma = LRC.charAt(i);
			}
														
			else { 
				char c = LRC.charAt(i);
				int ascii = (int)c;
				suma = suma ^ ascii;
			}
		}
		return suma;
	}

    public static void main(String[] args) {

        String puerto="";

		ServerSocket ServidorProcesador;
		String ret = "defecto";
		try
		{
			
			if (args.length < 2) {
				System.out.println("Debe indicar el puerto de escucha del Procesador y el nombre de su fichero config");
				System.out.println("Ejemplo: java Procesador puerto_proc config.txt");
				System.exit (1);
			}
			
		 		puerto = args[0];
				int num = Integer.parseInt(puerto);
				ServidorProcesador = new ServerSocket(num);
				System.out.println("Escucho el puerto " + num);
				
				String cad = "";
				boolean enviar = true;

                while(true) {
			
					Socket GateProc = ServidorProcesador.accept(); // Crea objeto
					System.out.println("Sirviendo a Pasarela...\n");

					cad = "";                
                    cad = leeSocket (GateProc, cad);
					System.out.println("Recibo la cadena: "+cad);		
					boolean ENTRO = true;
					if(cad.charAt(0)=='l'){
						if(cad.charAt(1)=='i'){
							if(cad.charAt(2)=='/'){
								ENTRO=false;
							}
						}
					}

					if(ENTRO == true){
						String v2[]= cad.split("<ETX>");
						String sLRC_gate = v2[1];
						String cad2=cad.substring(5);
						String v[] = cad2.split("<ETX>");
						String paraLRC = v[0];
						int LRC_gate = Integer.parseInt(sLRC_gate);
						int LRC_proc = calculaLRC(paraLRC);
						System.out.println("LRC de la cadena: "+LRC_proc);
						if(LRC_proc==LRC_gate) {
							System.out.println("Los LRC entre Gateway -> Procesador coinciden: cadena recibida con exito\n");
						}
						else {
							System.out.println("Los LRC entre Gateway -> Procesador no coinciden: error al pasar el mensaje\n");
						}

						FileReader cin = null;
						BufferedReader lee = null;
						FileWriter cout = null;

						String fich = null;
						String fichero = "";

						String archivo = "";
						int contador = 0;
						int NUM_MAX_CONFIG = 2;
						enviar = true;
						boolean javo_salte = false;
						//while (contador < NUM_MAX_CONFIG && javo_salte == false){
							//if (contador==0) archivo="config.txt";
							//if (contador==1) archivo="config2.txt";
							archivo = args[1];
							enviar = true;

							//==============  OPERACION AUTH  ============================
							if(cad.charAt(5)=='1'){

								System.out.println("Me llega la cadena: "+cad);
								
								Calendar fecha = new GregorianCalendar();
								String parametros[] = cad.split("&");
								String titular = parametros[1];
								String tarjeta = parametros[2];
								int mes = Integer.parseInt((parametros[3].split("="))[1]);
								int anio = Integer.parseInt("20"+(parametros[4].split("="))[1]);
								//int cvv = Integer.parseInt((parametros[5].split("="))[1]);
								String cvv = (parametros[5].split("="))[1];
								long importe = Long.parseLong((parametros[6].split("="))[1]);
								int cont = 0; //cuenta el codigo de pagos aceptados (0,1,2,3, ...)
								

								int ano_actual = fecha.get(Calendar.YEAR); //2020"
								int mes_actual = fecha.get(Calendar.MONTH) +1; //4
								int dia_actual = fecha.get(Calendar.DAY_OF_MONTH); //2

								String elproc = (cad.split("&"))[7];
								elproc = Character.toString(elproc.charAt(elproc.length()-1));


								String m_act = "", d_act="";
								if(mes_actual<=9) m_act="0"+mes_actual;
								else m_act=""+mes_actual;
								if(dia_actual<=9) d_act="0"+dia_actual;
								else d_act = ""+dia_actual;

								int floor=-1, upper=-1;
								boolean est = true;

								if(mes<=0 || mes>12 ){ 
									enviar = true; //la tarjeta caduca como maximo dentro de 5 anios desde hoy
									//ret="Ko. Denegada: Datos de la tarjeta invalidos (mes incorrecto)";
									ret = "auth/ko/mes_incorrecto";
								}
								else if (anio > ano_actual+5){
								enviar = true;
									//ret="Ko. Denegada: Datos de la tarjeta invalidos (la tarjeta no puede caducar dentro de mas de 5 anyos...)";
									ret = "auth/ko/dato_invalido";
								}

								else if(anio < ano_actual) {
									enviar=true; //tarjeta caducada
									//ret = "Ko. Denegada: Tarjeta caducada";
									ret = "auth/ko/caducado";
								}
								
								else if(enviar){
									cin = new FileReader(archivo);
									lee = new BufferedReader(cin);
									while ((fich = lee.readLine()) != null){
										String vector[] = fich.split("=");
										if(vector[0].equalsIgnoreCase("ID")){
							
											if(vector[1].equals(elproc)){
												javo_salte=true;
												fich = lee.readLine();
												vector = fich.split("=");
									
												if(vector[0].equalsIgnoreCase("Estado")){
													if(vector[1].equalsIgnoreCase("ON")){
										
														fich = lee.readLine();
														vector = fich.split("=");
														if(vector[0].equalsIgnoreCase("Floor")){
													
															floor = Integer.parseInt(vector[1]);
															enviar = true;
															fich = lee.readLine();	
															vector = fich.split("=");
															if(vector[0].equalsIgnoreCase("Upper")){
																upper = Integer.parseInt(vector[1]);
																
															}
														}
													}
													else {
														est = false;
													}
												}
											}
										}
									}
								
									if (est == false){

										enviar=true;
										//ret = "Ko. Denegada: Estado del Procesador desactivado";
										ret = "auth/ko/proc_apagado";
									}
									else if(importe < floor || importe > upper){

										enviar = true;
										//ret = "Ko. Denegada: Importe fuera de los limites permitidos";
										ret = "auth/ko/importe_invalido";
									}
									else {

										enviar = true; //se efectua el pago
										//ret = "Aceptada. Codigo de Autorizacion: "+ (cont++) +". "+d_act+"-"+m_act+"-"+ano_actual+". "+importe+" euros";
										ret = "auth/ok/cvv="+cvv+"/dia="+d_act+"/mes="+m_act+"/anyo="+ano_actual+"/importe="+importe;
									}
								}
							}

							//==============  OPERACION STATUS  ============================
							else if(cad.charAt(5)=='2'){ 

								boolean set = false;
								if (cad.split("&").length== 3) set = true;

								if(!set){ //MODO CONSULTA

									String churr[] = cad.split("&");
									String churr2[] = churr[1].split("<");
									String proc = churr2[0];  //proc = "procesador=1"
									String churr3[] = proc.split("="); 
									String elproc = churr3[1]; //elproc="1"
									String estado = "";
									String modificado = "";
																
									try {
										cin = new FileReader(archivo);
										lee = new BufferedReader(cin);
										boolean salir = false;
										String churro[] = null;
										String dev = "NO ENTRO EN EL BUCLE. FICHERO MAL";
								
										while ((fich = lee.readLine()) != null && salir == false){
										
											fichero = fichero + fich;	
											String vector[] = fich.split("=");
											if(vector.length!=2){
												System.out.println("DEBES ESCRIBIR BIEN EL FICHERO Procesadores.txt");
												salir=true;
												//***QUE DEVUELVO */
											}
											if(vector[0].equalsIgnoreCase("ID")){
											
												if(vector[1].equals(elproc)){
												javo_salte=true;
													fich = lee.readLine();
													vector = fich.split("=");
													if(vector[0].equalsIgnoreCase("Estado")){
														dev = vector[1];
														salir = true;
														enviar = true;
														ret = "status/ok/ver/"+dev;
													}
												}
											}
										}
										
										System.out.println("RET vale: "+ret);
										if(dev.equals("NO ENTRO EN EL BUCLE. FICHERO MAL")) enviar = false;
										
									}  catch(IOException e) {
										System.out.println("ERROR. Debe existir el fichero Bines.txt");
									}
									finally {
										try {
											if(cin != null) cin.close();
											if(lee != null) lee.close();
										} catch(IOException e) {System.out.println("ERROR. NO SE CIERRA");}
									}
								}
								else { //si tengo que hacer set

									String churr[] = cad.split("&");
									String churr2[] = churr[2].split("<");
									String proc = churr2[0];  //proc = set=1
									String churr3[] = proc.split("="); 
									String setter = churr3[1]; //setter=1
									String estado = "";
									String modificado = "";
									String elproc = (churr[1].split("="))[1]; //elproc=1 pq procesador=1

									if(setter.equals("0")) estado = "OFF"; //lo tengo q poner en OFF
									else if (setter.equals("1")) estado = "ON"; 
									else System.out.println("EL ESTADO SOLO PUEDE CAMBIAR A 0(OFF) o a 1(ON)");


									try {
										cin = new FileReader(archivo);
										lee = new BufferedReader(cin);
										boolean salir = false;
										String churro[] = null;
										String dev = "NO ENTRO EN EL BUCLE. FICHERO MAL";
										while ((fich = lee.readLine()) != null && salir == false){
											fichero = fichero + fich;	
											String vector[] = fich.split("=");
											if(vector.length!=2){
												System.out.println("DEBES ESCRIBIR BIEN EL FICHERO Procesadores.txt");
												salir=true;
											}
											if(vector[0].equalsIgnoreCase("ID")){
												
												if(vector[1].equals(elproc)){
													javo_salte=true;
													fich = lee.readLine();
													vector = fich.split("=");
													if(vector[0].equalsIgnoreCase("Estado")){
														dev = vector[1];
														salir = true;
														enviar = true;
													}
												}
											}
										}
										if(!salir){
											System.out.println(dev); //ERROR NO ENTRO EN EL CONDICIONAL, FICHERO MAL (Falta estado=...)
											
										}
										else if(dev.equals(estado)){ //ERROR NO SE PUEDE ACTUALIZAR EL ESTADO YA ERA ON O OFF
											dev = "status/ko/set/"+estado;
											//dev = "No se puede actualizar. El estado ya era "+estado;
											System.out.println("No se puede actualizar. El estado ya era "+estado);
										}
										else {
											
											cin = new FileReader(archivo);
											lee = new BufferedReader(cin);
											
											while ((fich = lee.readLine()) != null){
												boolean entra = false;
												String vector[] = fich.split("=");
												
												if(vector[0].equalsIgnoreCase("ID")){
													if(vector[1].equals(elproc)){ //si el navegador del fichero ID=1 es el mismo que el que tengo que modificar (elproc)
														//javo_salte=true;
														modificado = modificado + fich+"\r\n";
														entra = true;
														fich=lee.readLine();
														vector = fich.split("=");
														if(vector[0].equalsIgnoreCase("Estado")){
															vector[1] = estado;
															modificado = modificado + "Estado="+estado+"\r\n";
														}
													}
												}
												if(entra==false) modificado = modificado + fich + "\r\n";
											}

											cout = new FileWriter(archivo);
											cout.write(modificado);
											cout.close();
											//dev = "OK procesador numero "+ elproc +" cambiado de "+ dev +" a "+estado;
											dev = "status/ok/set/"+setter;										
										}
										ret=dev;
										if(ret.equals("NO ENTRO EN EL BUCLE. FICHERO MAL")) enviar = false;
									}  catch(IOException e) {
										System.out.println("ERROR. Debe existir el fichero Bines.txt");
									}
									finally {
										try {
											if(cin != null) cin.close();
											if(lee != null) lee.close();
										} catch(IOException e) {System.out.println("ERROR. NO SE CIERRA");}
									}								
								}
							}

							//=============   OPERACION FL ===================
							else if(cad.charAt(5)=='4'){
								System.out.println("Operacion 4");
								boolean set = false;
								if (cad.split("&").length== 3) set = true;

								if(!set){ //MODO CONSULTA

									String churr[] = cad.split("&");
									String churr2[] = churr[1].split("<");
									String proc = churr2[0];  //proc = "procesador=1"
									String churr3[] = proc.split("="); 
									String elproc = churr3[1]; //elproc="1"
									String estado = "";
									String modificado = "";
																
									try {
										cin = new FileReader(archivo);
										lee = new BufferedReader(cin);
										boolean salir = false;
										String churro[] = null;
										String dev = "NO ENTRO EN EL BUCLE. FICHERO MAL";
								
										while ((fich = lee.readLine()) != null && salir == false){
										
											fichero = fichero + fich;	
											String vector[] = fich.split("=");
											if(vector.length!=2){
												System.out.println("DEBES ESCRIBIR BIEN EL FICHERO Procesadores.txt");
												salir=true;
												//***QUE DEVUELVO */
											}
											if(vector[0].equalsIgnoreCase("ID")){
												if(vector[1].equals(elproc)){
													javo_salte=true;
													fich = lee.readLine();
													fich = lee.readLine();
													vector = fich.split("=");
													if(vector[0].equalsIgnoreCase("Floor")){
														dev = vector[1];
														salir = true;
														enviar = true;
													}
												}
											}
										}
										if(dev.equals("NO ENTRO EN EL BUCLE. FICHERO MAL")) enviar = false;

										ret = "fl/ok/"+dev;
										//ret = "El procesador numero "+elproc+" tiene como limite inferior: "+dev;
										System.out.println("RET vale: "+ret);
										
									}  catch(IOException e) {
										System.out.println("ERROR. Debe existir el fichero Bines.txt");
									}
									finally {
										try {
											if(cin != null) cin.close();
											if(lee != null) lee.close();
										} catch(IOException e) {System.out.println("ERROR. NO SE CIERRA");}
									}
								}
								else { //si tengo que hacer set

									String churr[] = cad.split("&");
									String churr2[] = churr[2].split("<");
									String proc = churr2[0];  //proc = set=1
									String churr3[] = proc.split("="); 
									String setter = churr3[1]; //setter=600
									String estado = "";
									String modificado = "";
									String elproc = (churr[1].split("="))[1]; //elproc=1 pq procesador=1
									boolean rango = false;
									boolean era_igual = false;
									
									try {
										cin = new FileReader(archivo);
										lee = new BufferedReader(cin);
										boolean salir = false;
										String churro[] = null;
										String dev = "NO ENTRO EN EL BUCLE. FICHERO MAL";
										while ((fich = lee.readLine()) != null && salir == false){
											fichero = fichero + fich;	
											String vector[] = fich.split("=");
											if(vector.length!=2){
												System.out.println("DEBES ESCRIBIR BIEN EL FICHERO Procesadores.txt");
												salir=true;
											}
											if(vector[0].equalsIgnoreCase("ID")){
												System.out.println("Procesador: "+elproc +" y ademas vector "+vector[1]);
												if(vector[1].equals(elproc)){
													javo_salte=true;
													fich = lee.readLine();
													fich = lee.readLine();
													vector = fich.split("=");
													if(vector[0].equalsIgnoreCase("Floor")){
														dev = vector[1]; //floor = dev y mi floor lo quiero poner en = setter
														if(dev.equals(setter)){
															era_igual=true;
														}
														else{

															fich = lee.readLine();
															vector = fich.split("=");
															int superior = Integer.parseInt(vector[1]);

															System.out.println("Superior: "+superior);
															System.out.println("Lo pongo en: "+setter);
															if (Integer.parseInt(setter) > superior){
																rango=true;
															}
														}
														salir = true;
														enviar = true;
													}
												}
											}
										}
										if(!salir){
											System.out.println(dev); //ERROR NO ENTRO EN EL CONDICIONAL, FICHERO MAL (Falta estado=...)
											enviar=false;
										}
										else if(era_igual){

											dev = "fl/ko/set_igual";
											//dev = "No se puede actualizar. El limite inferior ya vale el valor que estabas intentando ponerle";
										}

										else if(rango){ //ERROR NO SE PUEDE ACTUALIZAR 
											dev = "fl/ko/set_inferior";
											//dev = "No se puede actualizar. El limite inferior no puede ser mayor que el superior";
											System.out.println("No se puede actualizar. El limite inferior no puede ser mayor que el superior");
										}
										else { //voy a actualizar el fichero
											
											cin = new FileReader(archivo);
											lee = new BufferedReader(cin);
											
											System.out.println("El fichero:");
											while ((fich = lee.readLine()) != null){
												boolean entra = false;
												String vector[] = fich.split("=");
												
												if(vector[0].equalsIgnoreCase("ID")){
													if(vector[1].equals(elproc)){ //si el navegador del fichero ID=1 es el mismo que el que tengo que modificar (elproc)
														javo_salte=true;
														modificado = modificado + fich+"\r\n";
														entra = true;
														fich=lee.readLine();
														vector = fich.split("=");
														modificado = modificado + fich+"\r\n";
														fich=lee.readLine();
														vector = fich.split("=");
														if(vector[0].equalsIgnoreCase("Floor")){
															vector[1] = setter;
															modificado = modificado + "Floor="+setter+"\r\n";
														}
													}
												}
												if(entra==false) {
													modificado = modificado + fich + "\r\n";
												}
											}

											cout = new FileWriter(archivo);
											cout.write(modificado);
											cout.close();

											dev = "fl/ok/set/"+setter;
											//dev = "OK del procesador numero "+ elproc +" ha cambiado el limite inferior a "+setter;										
										}
										//if(dev.equals("NO ENTRO EN EL BUCLE. FICHERO MAL")) enviar = false;
										ret=dev;
									}  catch(IOException e) {
										System.out.println("ERROR. Debe existir el fichero Bines.txt");
									}
									finally {
										try {
											if(cin != null) cin.close();
											if(lee != null) lee.close();
										} catch(IOException e) {System.out.println("ERROR. NO SE CIERRA");}
									}
								}
							}

							//=============   OPERACION UL ===================
							else if(cad.charAt(5)=='3'){
								System.out.println("Operacion 3");
								boolean set = false;
								if (cad.split("&").length== 3) set = true;

								if(!set){ //MODO CONSULTA

									String churr[] = cad.split("&");
									String churr2[] = churr[1].split("<");
									String proc = churr2[0];  //proc = "procesador=1"
									String churr3[] = proc.split("="); 
									String elproc = churr3[1]; //elproc="1"
									String estado = "";
									String modificado = "";
									
								
									try {
										cin = new FileReader(archivo);
										lee = new BufferedReader(cin);
										boolean salir = false;
										String churro[] = null;
										String dev = "NO ENTRO EN EL BUCLE. FICHERO MAL";
								
										while ((fich = lee.readLine()) != null && salir == false){
										
											fichero = fichero + fich;	
											String vector[] = fich.split("=");
											if(vector.length!=2){
												System.out.println("DEBES ESCRIBIR BIEN EL FICHERO Procesadores.txt");
												salir=true;
												//***QUE DEVUELVO */
											}
											if(vector[0].equalsIgnoreCase("ID")){
												if(vector[1].equals(elproc)){
													javo_salte=true;
													fich = lee.readLine();
													fich = lee.readLine();
													fich = lee.readLine();
													vector = fich.split("=");
													if(vector[0].equalsIgnoreCase("Upper")){
														dev = vector[1];
														salir = true;
														enviar = true;
													}
												}
											}
										}
										if(dev.equals("NO ENTRO EN EL BUCLE. FICHERO MAL")) enviar = false;

										ret = "ul/ok/"+dev;
										//ret = "El procesador numero "+elproc+" tiene como limite superior: "+dev;
										System.out.println("RET vale: "+ret);
										
									}  catch(IOException e) {
										System.out.println("ERROR. Debe existir el fichero Bines.txt");
									}
									finally {
										try {
											if(cin != null) cin.close();
											if(lee != null) lee.close();
										} catch(IOException e) {System.out.println("ERROR. NO SE CIERRA");}
									}
								}
								else { //si tengo que hacer set

									String churr[] = cad.split("&");
									String churr2[] = churr[2].split("<");
									String proc = churr2[0];  //proc = set=1
									String churr3[] = proc.split("="); 
									String setter = churr3[1]; //setter=600
									String estado = "";
									String modificado = "";
									String elproc = (churr[1].split("="))[1]; //elproc=1 pq procesador=1
									boolean rango = false;
									boolean era_igual = false;								

									try {
										cin = new FileReader(archivo);
										lee = new BufferedReader(cin);
										boolean salir = false;
										String churro[] = null;
										String dev = "NO ENTRO EN EL BUCLE. FICHERO MAL";
										while ((fich = lee.readLine()) != null && salir == false){
											fichero = fichero + fich;	
											String vector[] = fich.split("=");
											if(vector.length!=2){
												System.out.println("DEBES ESCRIBIR BIEN EL FICHERO Procesadores.txt");
												salir=true;
											}
											if(vector[0].equalsIgnoreCase("ID")){
												//System.out.println("Procesador: "+elproc +" y ademas vector "+vector[1]);
												if(vector[1].equals(elproc)){
													javo_salte=true;

													fich = lee.readLine();
													fich = lee.readLine();

													vector = fich.split("=");
													int lim_inferior = Integer.parseInt(vector[1]);

													fich = lee.readLine();
						
													vector = fich.split("=");
													if(vector[0].equalsIgnoreCase("Upper")){
														dev = vector[1]; //upper = dev y mi upper lo quiero poner  = setter
														
														if(dev.equals(setter)){
			
															era_igual=true;
														}
														else{
															if (Integer.parseInt(setter) < lim_inferior){
																rango=true;
															}
														}
														salir = true;
														enviar = true;
													}												
												}
											}
										}
									
										if(!salir){
											System.out.println(dev); //ERROR NO ENTRO EN EL CONDICIONAL, FICHERO MAL (Falta estado=...)
											enviar = false;
										}
										else if(era_igual){

											dev = "ul/ko/set_igual";
											//dev = "No se puede actualizar. El limite superior ya vale el valor que estabas intentando ponerle";
											enviar = true;
										}

										else if(rango){ //ERROR NO SE PUEDE ACTUALIZAR 
											dev = "ul/ko/set_superior";
											//dev="No se puede actualizar. El limite superior no puede ser menor que el inferior";
											System.out.println("No se puede actualizar. El limite superior no puede ser menor que el inferior");
										}
										else { //voy a actualizar el fichero
											
											cin = new FileReader(archivo);
											lee = new BufferedReader(cin);
											
											System.out.println("El fichero:");
											while ((fich = lee.readLine()) != null){
												boolean entra = false;
												String vector[] = fich.split("=");
												

												if(vector[0].equalsIgnoreCase("ID")){
													if(vector[1].equals(elproc)){ //si el navegador del fichero ID=1 es el mismo que el que tengo que modificar (elproc)
														javo_salte=true;
														modificado = modificado + fich+"\r\n";
														entra = true;
														fich=lee.readLine();
														vector = fich.split("=");
														modificado = modificado + fich+"\r\n";
														fich=lee.readLine();
														modificado = modificado + fich+"\r\n";
														fich=lee.readLine();
														vector = fich.split("=");
														if(vector[0].equalsIgnoreCase("Upper")){
															vector[1] = setter;
															modificado = modificado + "Upper="+setter+"\r\n";
														}
													}
												}
												if(entra==false) {
													modificado = modificado + fich + "\r\n";
												}
											}
											
											cout = new FileWriter(archivo);
											cout.write(modificado);
											cout.close();
											//dev = "OK del procesador numero "+ elproc +" ha cambiado el limite superior de "+ dev +" a "+setter;
											dev = "ul/ok/set/"+setter;
										}
										
										ret=dev;
									}  catch(IOException e) {
										System.out.println("ERROR. Debe existir el fichero Bines.txt");
									}
									finally {
										try {
											if(cin != null) cin.close();
											if(lee != null) lee.close();
										} catch(IOException e) {System.out.println("ERROR. NO SE CIERRA");}
									}
								}
								}				
							if(enviar==false){
								ret = "error";
							}
							int suma = calculaLRC(ret);
							ret = "<STX>"+ret+"<ETX>"+suma;
							ret = ret + "\r\n";

							escribeSocket(GateProc, ret);
							System.out.println("\nDevuelvo a Gateway: "+ret);
							GateProc.close();
							System.out.println("===============");
						
					}
					else { //si tengo un listado

						FileReader cin = null;
						BufferedReader lee = null;
						String fich = null;
						String fichero = "";

						cin = new FileReader(args[1]);
						lee = new BufferedReader(cin);

						boolean bien = false;
						String procesador="";
						String estado = "";
						String floor = "";
						String upper = "";

						while ((fich = lee.readLine()) != null){
						String vector[] = fich.split("=");
							if(vector[0].equalsIgnoreCase("ID")){
								procesador = vector[1];
								fich = lee.readLine();
								vector = fich.split("=");
									
								if(vector[0].equalsIgnoreCase("Estado")){
									if(vector[1].equalsIgnoreCase("ON")){
										estado="ON";
									}
									else {
										estado = "OFF";
									}
										
									fich = lee.readLine();
									vector = fich.split("=");
									if(vector[0].equalsIgnoreCase("Floor")){													
										floor = vector[1];								
										fich = lee.readLine();	
										vector = fich.split("=");
										if(vector[0].equalsIgnoreCase("Upper")){
											upper = vector[1];
											bien = true;
																
										}
									}
								}								
							}
						}

						if(bien) {

							 ret = procesador + "/" + estado + "/"+ floor + "/"+upper+"\r\n";
						}
						else {

							System.out.println("Ha habido algun problema con algun fichero Config.txt al mostrar el listado de procesadores");
							 ret = "Ha habido algun problema con algun fichero Config.txt al mostrar el listado de procesadores\r\n";
						}
						escribeSocket(GateProc, ret);
							System.out.println("Escrribo para gateway: "+ret);
					}


				}
			
		}
    
	catch(Exception e){
		System.out.println("Error: " + e.toString());		
	}
    System.out.println();System.out.println();System.out.println();System.out.println();
}}

