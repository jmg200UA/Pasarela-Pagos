import java.lang.Exception;
import java.net.Socket;
import java.io.*;
import java.net.*;
import java.util.*;

public class HiloServidor extends Thread {

	private Socket skCliente;
	private int puerto_escribo;
	private String ip_escribo;
	private int MAX_CLIENTES;
	//private ServerSocket Servidor2;
	//private int activos;
	//private Lock cerrar = new ReentrantLock();

	public HiloServidor(Socket p_cliente, int num, int n2, String ip){
		this.skCliente = p_cliente;
		puerto_escribo = num;
		MAX_CLIENTES = n2;
		ip_escribo = ip;
		//this.Servidor2 = Gateway;
		//this.activos = n;
	}
	

	/*
	* Lee datos del socket. Supone que se le pasa un buffer con hueco 
	*	suficiente para los datos. Devuelve el numero de bytes leidos o
	* 0 si se cierra fichero o -1 si hay error.
	*/
	public String leeSocket (Socket p_sk, String p_Datos)
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

	public void escribeSocket (Socket p_sk, String p_Datos)
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
	
    public void run() {
		
		int resultado=0;
		String cad="";

		String html_cod = "";
		int tam = 0;
		//cerrar.lock();

		//try {
System.out.println("El numero maximo de clientes simultaneos es: "+MAX_CLIENTES);
System.out.println(ServidorConcurrente.actuales);

		if(ServidorConcurrente.actuales < MAX_CLIENTES){
			try {
			
					//recibo de HTTPServer
					String recibo="";
					cad = this.leeSocket (skCliente, cad);
					String[] array = cad.split("/");
					boolean favicon = false;
					boolean pasarela = false;
					for(int i=0; i<array.length; i++){
						//System.out.println("Cadena "+i+":  "+array[i]);
						if(array[i].equalsIgnoreCase("GatewaySD")) pasarela = true;
						if(array[i].equals("favicon.ico HTTP")) favicon = true;
					}
					
					//if(favicon==false) sleep(50); //si es favicon que se espere 50 ms para que entre primero la peticion buena


					System.out.println("");
					System.out.println("Recibo del cliente la cadena: "+cad);
					
					
					
					if(cad.equals("GET / HTTP/1.1") == false && cad.equals("GET /GatewaySD/index.html HTTP/1.1") == false){ /* *** Esto es si no me escribe localhost */
						if(array[0].equals("GET ")){
							if(cad.length()<1000){
								
								boolean NO_ENTRO = false;
									if(favicon == false){
										if(pasarela) {
											sleep(1000);
											System.out.println("Hay pasarela");
											System.out.println("");
											//Abro Socket de Servidor-Gateway
											System.out.println("Escribo a "+ip_escribo+" : "+puerto_escribo);
											Socket ServerGate = new Socket(ip_escribo,puerto_escribo); //****OJOOOO quitar esto */


											//modifico lo q envio
											String envio_gate = "";
											String oper = array[2]; //oper = status?metodo=2&set=1
											String nombre[] = oper.split("\\?");
											if(nombre[0].equalsIgnoreCase("auth")) envio_gate = envio_gate + "1&";
											else if(nombre[0].equalsIgnoreCase("status")) envio_gate = envio_gate + "2&";
											else if(nombre[0].equalsIgnoreCase("ul")) envio_gate = envio_gate + "3&";
											else if(nombre[0].equalsIgnoreCase("fl")) envio_gate = envio_gate + "4&";
											else {
												System.out.println("No llega bien la operacion a realizar (auth/status/fl,ul)"); //***COMO COMPRUEBO ESTO SE PUEDE? */ //claro me lo mandan desde buscador escrito
												NO_ENTRO = true;
											}

											if(NO_ENTRO==false){
												String[] parametros = nombre[1].split(" ");
												envio_gate = envio_gate + parametros[0];

												/* Almaceno en suma el valor LRC*/
												int suma = calculaLRC(envio_gate);
												envio_gate = "<STX>"+envio_gate+"<ETX>"+suma;
												envio_gate = envio_gate + "\r\n";

												//Envio a Gateway
												
												escribeSocket(ServerGate, envio_gate);
												
											
												System.out.println("He enviado a GATEWAY la cadena: "+cad);
												System.out.println("LRC de la cadena que paso: "+suma);
												System.out.println("");
												//ServerGate.close(); 

												//recibo de Gateway
												
												//ServerGate = Servidor2.accept();
												//recibo = leeSocket(ServerGate, recibo);
												//System.out.println("Recibo de Gateway: "+recibo);

												recibo = leeSocket(ServerGate, recibo);
												String recib2 = recibo;
												ServerGate.close();
												System.out.println("Recibo la cadena: "+recibo);

												recibo = recibo.substring(5);
												String p[] = recibo.split("<ETX>");
												recibo = p[0];
									


												String v2[]= recib2.split("<ETX>");
												String sLRC_HTTP = v2[1];
												String cad2=recib2.substring(5);
												String v[] = cad2.split("<ETX>");
												String paraLRC = v[0];
												System.out.println("Recibo la cadena: "+paraLRC);
												int LRC_HTTP = Integer.parseInt(sLRC_HTTP);
												int LRC_gate = calculaLRC(paraLRC);
												System.out.println("LRC de la cadena: "+LRC_gate);
												if(LRC_gate==LRC_HTTP) {
													System.out.println("Los LRC entre Gateway -> HTTPServer coinciden: cadena recibida con exito\n");
												}
												else {
													System.out.println("Los LRC entre Gateway -> HTTPServer no coinciden: error al pasar el mensaje\n");
												}

											
			
												//Desempaqueto 
												String mando="";
												if(recibo.equals("error")) mando="El fichero Config.txt tiene un formato incorrecto";
												else if(recibo.equals("error/bines")) mando="El formato del fichero Bines.txt es incorrecto. Debe contener navegadores para todos los numeros de tarjeta y cada uno debe asociarlo a un procesador que exista. Modificalo";
												else if (recibo.equals("error/proc")) {
													mando="El procesador que has indicado no existe";
												}
												//else if (recibo.equals("error/proc2")) mando="Problema interno";
												else {

													String para[] = recibo.split("/");
													if(para[0].equals("auth")){

														if(para[1].equals("ok")){
															String cvv_pa[] = para[2].split("=");
															String cvv=(cvv_pa[1]);
															String dia_pa[] = para[3].split("=");
															String dia=(dia_pa[1]);
															String mes_pa[] = para[4].split("=");
															String mes=(mes_pa[1]);
															String any_pa[] = para[5].split("=");
															String any = (any_pa[1]);
															String imp_pa[] = para[6].split("=");
															String imp=(imp_pa[1]);

															mando="Aceptada<br> Codigo de Autorizacion: "+ cvv +".<br>Fecha del pago: "+dia+"-"+mes+"-"+any+". <br>Importe: "+imp+" euros";
														}
														else{
															if(para[2].equals("mes_incorrecto")){
																mando="Ko. Denegada: Datos de la tarjeta invalidos (mes incorrecto)";
															}
															else if(para[2].equals("dato_invalido")){
																mando="Ko. Denegada: Datos de la tarjeta invalidos (la tarjeta no puede caducar dentro de mas de 5 anyos...)";
															}
															else if(para[2].equals("caducado")){
																mando="Ko. Denegada: Tarjeta caducada";

															}
															else if(para[2].equals("proc_apagado")){
																mando="Ko. Denegada: Estado del Procesador desactivado";
															}
															else if (para[2].equals("importe_invalido")){
																mando="Ko. Denegada: Importe fuera de los limites permitidos";
															}

														}
														
													} else if (para[0].equals("status")){

														String e="";
														if(para[3].equalsIgnoreCase("ON") || para[3].equalsIgnoreCase("1")) e="activo";
														else e="apagado";
														System.out.println("e vale "+e);

														if(para[1].equals("ok")){
															
															
															if(para[2].equals("ver")){
																mando="Ok. Procesador " + e;
															}
															else if (para[2].equals("set")){
																mando= "OK el procesador ahora esta " + e;
															}
														}
														else if (para[1].equals("ko")){
																

															mando = "No se puede actualizar. El procesador ya estaba " + e;
														}

													} else if (para[0].equals("ul")){

														if(para[1].equals("ok")){

															if(para[2].equals("set")){

																long lim = Long.parseLong(para[3]);
																mando = "OK. El limite superior del procesador ha cambiado a "+lim;
															}
															else {
																long lim = Long.parseLong(para[2]);
																mando = "El procesador tiene como limite superior: "+lim;
															}
														}
														else if (para[1].equals("ko")){

															if(para[2].equalsIgnoreCase("set_igual")){
																
																mando="No se puede actualizar. El limite superior ya vale el valor que estabas intentando ponerle";
															}
															else if (para[2].equalsIgnoreCase("set_superior")){
																mando="No se puede actualizar. El limite superior no puede ser menor que el inferior";
															}
														}

													} else if (para[0].equals("fl")){

														if(para[1].equals("ok")){

															if(para[2].equals("set")){

																long lim = Long.parseLong(para[3]);
																mando = "OK. El limite inferior del procesador ha cambiado a "+lim;
															}
															else {
																long lim = Long.parseLong(para[2]);
																mando = "El procesador tiene como limite inferior: "+lim;
															}
														}
														else if (para[1].equals("ko")){

															if(para[2].equalsIgnoreCase("set_igual")){
																
																mando="No se puede actualizar. El limite inferior ya vale el valor que estabas intentando ponerle";
															}
															else if (para[2].equalsIgnoreCase("set_inferior")){
																mando="No se puede actualizar. El limite inferior no puede ser mayor que el superior";
															}
														}
													}
												}

												System.out.println("Le envio al cliente: "+mando);
												//Envio a cliente
												html_cod = "<html><h1></h1></html>";
												tam = html_cod.length() +mando.length();
												this.escribeSocket (skCliente, "HTTP1.1 200 OK \nContent-Type: text/html; charset=utf-8 Content-Lenght: "+tam+" Connection: close Server: Servidor HTTP SD\n\n "+"<html><h1>"+mando+"</h1></html>"+"\n");
																															
												skCliente.close();
												System.out.println("");System.out.println("");
											}	
											else{ //o sea si no entro
												html_cod = "<html><body><h1>Error 400: Peticion incorrecta. Comprueba el formato de tu peticion </h1></body></html>";
												tam = html_cod.length();
												this.escribeSocket (skCliente, "HTTP1.1 404 Not Found \nContent-Type: text/html; charset=utf-8 Content-Lenght: "+tam+" Connection: close Server: Servidor HTTP SD\n\n <html><body><h1>Error 400: Peticion incorrecta. Comprueba el formato de tu peticion </h1></body></html>\n");
												skCliente.close();

											}
										}
										else if (favicon == false){
											System.out.println("No hay pasarela");

											
											String[] pagina = cad.split(" ");			
											pagina[1] = pagina[1].substring(1, pagina[1].length());

											FileReader cin = null;
											BufferedReader lee = null;
											String fich = null;
											String fichero = "";
										
																							
												try {

													System.out.println("Pagina[1] vale: "+pagina[1]);

													cin = new FileReader(pagina[1]);
													lee = new BufferedReader(cin);
												
													while ((fich = lee.readLine()) != null){
														fichero = fichero + fich;											
												
														tam = fichero.length();

														this.escribeSocket (skCliente, "HTTP1.1 200 OK \nContent-Type: text/html; charset=utf-8 Content-Lenght: "+tam+" Connection: close Server: Servidor HTTP SD\n\n "+fichero+"\n");
													}

													System.out.println("El fichero leido es: "+fichero+"\n");
													
												} 
												catch(IOException e) {
													System.out.println("El cliente está buscando un FICHERO NO ENCONTRADO");
													html_cod = "<html><h1>Error 404: Recurso no encontrado</h1></html>";
													tam = html_cod.length();
													this.escribeSocket (skCliente, "HTTP1.1 404 Not Found \nContent-Type: text/html; charset=utf-8 Content-Lenght: "+tam+" Connection: close Server: Servidor HTTP SD\n\n  <html><h1>Error 404: Recurso no encontrado</h1></html>\n");

												}
												finally {
													try {
														if(cin != null) cin.close();
														if(lee != null) lee.close();
													} catch(IOException e) {System.out.println("Error al cerrar el fichero");}
												}	
											

											System.out.println("");System.out.println("");	

											//this.escribeSocket (skCliente, "HTTP1.1 200 OK \nContent-Type: text/html; charset=utf-8\n\n <html><body><h1>OK</h1></body></html>\n");						
											skCliente.close();
											

										}
										else {
											skCliente.close();
										}
									}
									
							}
							
							else{ //Si la peticion es MUY grande

								html_cod = "<html><body><h1>Error 413: Solo se aceptan peticiones con menos de 1000 caracteres </h1></body></html>";
								tam = html_cod.length();
								this.escribeSocket (skCliente, "HTTP1.1 413 Request Entity Too Large \nContent-Type: text/html; charset=utf-8 Content-Lenght: "+tam+" Connection: close Server: Servidor HTTP SD\n\n <html><body><h1>Error 413: Solo se aceptan peticiones con menos de 1000 caracteres </h1></body></html>\n");
								skCliente.close();
							}
							
						}
						else { //Si no me llega el GET
							html_cod = "<html><body><h1> Error 405: Solo se aceptan peticiones realizadas mediante el metodo GET </h1></body></html>";
							tam = html_cod.length();
							this.escribeSocket (skCliente, "HTTP1.1 405 Method not Allowed \nContent-Type: text/html; charset=utf-8 Content-Lenght: "+tam+" Connection: close Server: Servidor HTTP SD\n\n <html><body><h1> Error 405: Solo se aceptan peticiones realizadas mediante el metodo GET </h1></body></html>\n");
							skCliente.close();
						}
					}
					else if (cad.equals("GET / HTTP/1.1")){ //si el usuario escribe http://localhost:8080

							FileReader cin2 = null;
							BufferedReader lee2 = null;
							String fich2 = null;
							String fichero2 = "";
							
							
						try {

							
							cin2 = new FileReader("index.html");
							lee2 = new BufferedReader(cin2);
													
							while ((fich2 = lee2.readLine()) != null){
								fichero2 = fichero2 + fich2;	
								tam=fichero2.length();
								this.escribeSocket (skCliente, "HTTP1.1 200 OK \nContent-Type: text/html; charset=utf-8 Content-Lenght: "+tam+" Connection: close Server: Servidor HTTP SD\n\n "+fichero2+"\n");
							}

							System.out.println("El fichero leido es: "+fichero2+"\n");
							skCliente.close();
													
						} 
						catch(IOException e) {
							System.out.println("El cliente está buscando un FICHERO NO ENCONTRADO");
							html_cod = "<html><h1>Error 404: Recurso no encontrado</h1></html>";
							tam = html_cod.length();
							this.escribeSocket (skCliente, "HTTP1.1 404 Not Found \nContent-Type: text/html; charset=utf-8 Content-Lenght: "+tam+" Connection: close Server: Servidor HTTP SD\n\n  <html><h1>Error 404: Recurso no encontrado</h1></html>\n");

						}
						finally {
							try {
								if(cin2 != null) cin2.close();
								if(lee2 != null) lee2.close();
								} 
							catch(IOException e) {System.out.println("Error al cerrar el fichero");}
						}						
						
					}
					else { //Si me llega el listado de Procesadores para el index

						//System.out.println("VALE ESTO AQUI");

						System.out.println("Escribo a "+ip_escribo+" : "+puerto_escribo);
						Socket ServerGate = new Socket(ip_escribo,puerto_escribo); //****OJOOOO quitar esto */
						String lis = "listado\r\n";
						int suma =calculaLRC(lis);
						//lis = "<STX>"+lis+"<ETX>"+suma;

						System.out.println("Escribo a gate: "+lis);
						escribeSocket(ServerGate, lis);
						

						String rec = "";
						rec = leeSocket(ServerGate, rec);

						html_cod = "Listado de procesadores<br>";
						tam = html_cod.length() + rec.length();
						this.escribeSocket (skCliente, "HTTP1.1 200 OK \nContent-Type: text/html; charset=utf-8 Content-Lenght: "+tam+" Connection: close Server: Servidor HTTP SD\n\n "+"Listado de procesadores<br>"+rec+"\n");
						skCliente.close();

					}			
			}
		
			catch (Exception e) {
			
			html_cod="<html><body><h1>Error 409: Error interno. Intentalo de nuevo mas tarde... No has arrancado algún componente o se esta escuchando por un puerto incorrecto</h1></body></html>";
			tam = html_cod.length();
			this.escribeSocket (skCliente, "HTTP1.1 409 Internal Server Error \nContent-Type: text/html; charset=utf-8 Content-Lenght: "+tam+" Connection: close Server: Servidor HTTP SD\n\n <html><body><h1>Error 409: Error interno. Intentalo de nuevo mas tarde... No has arrancado algún componente o se esta escuchando por un puerto incorrecto</h1></body></html>\n");	
			try{skCliente.close();} catch(Exception e2){};
			}
		}
		else {

			//System.out.println("Sobrecarga de clientes. El cliente ha sido avisado para que pruebe mas tarde");
			html_cod="<html><body><h1>Error 500: Error interno. Hay demasiados clientes que han mandado una peticion al mismo tiempo. Intentalo de nuevo mas tarde... </h1></body></html>";
			tam = html_cod.length();
			this.escribeSocket (skCliente, "HTTP1.1 500 Internal Server Error \nContent-Type: text/html; charset=utf-8 Content-Lenght: "+tam+" Connection: close Server: Servidor HTTP SD\n\n <html><body><h1>Error 500: Error interno. Hay demasiados clientes que han mandado una peticion al mismo tiempo. Intentalo de nuevo mas tarde... </h1></body></html>\n");
			try{
			skCliente.close();
			}catch(Exception e){

			}
		}
		//}
		//finally {

		//	cerrar.unlock();
			
		//}	
		ServidorConcurrente.actuales--;
		System.out.println("====================");
      }
}
