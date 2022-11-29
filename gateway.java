import java.lang.Exception;
import java.net.Socket;
import java.io.*;
import java.net.*;


public class gateway {

	private Socket ServerGate;

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
		ServerSocket ServidorGateway;
		ServerSocket ServidorGateway2;
		String recibo = "";

		try
		{
			
			if (args.length < 1) {
				System.out.println("Debe indicar el puerto de escucha del Gateway y el de Procesador");
				System.out.println("Ejemplo: java ServidorConcurrente puerto_gate");
				System.exit (1);
			}
			
				puerto = args[0];
				//int puerto_escribe = Integer.parseInt(args[1]);
				//int num = Integer.parseInt(puerto)+1;
				//ServidorGateway = new ServerSocket(num);
				ServidorGateway = new ServerSocket(Integer.parseInt(puerto));
				System.out.println("Escucho el puerto " + puerto);

			
				int oye=0;
                while(true) {
					
					recibo="";
					String cad = "";
					String dev = "";


					//Leo de HTTPServer
					Socket ServerGate = ServidorGateway.accept(); 
					System.out.println("Sirviendo a HTTPServer...\n");
					cad = leeSocket (ServerGate, cad);

					if(!cad.equals("listado")){
						String v2[]= cad.split("<ETX>");
						String sLRC_HTTP = v2[1];
						String cad2=cad.substring(5);
						String v[] = cad2.split("<ETX>");
						String paraLRC = v[0];
						System.out.println("Recibo la cadena: "+paraLRC);
						int LRC_HTTP = Integer.parseInt(sLRC_HTTP);
						int LRC_gate = calculaLRC(paraLRC);
						System.out.println("LRC de la cadena: "+LRC_gate);
						if(LRC_gate==LRC_HTTP) {
							System.out.println("Los LRC entre HTTPServer -> Gateway coinciden: cadena recibida con exito\n");
						}
						else {
							System.out.println("Los LRC entre HTTPServer -> Gateway no coinciden: error al pasar el mensaje\n");
						}
						
					
						//String LRC[]=cad.split("<STX>");
						//if (LRC[1].equals("1")){ //** */
						//}

						boolean envio_gate = true;
						FileReader cin = null;
						BufferedReader lee = null;
						String fich = null;
						String fichero = "";
						String numero="0";
						String[] a2 = cad.split("&");
						boolean salir = false;
						String churro[] = null;
						String naveg = "";
						String IP = "";
						String PORT = "";
						String cad_toProcesador = cad;

						if(cad.charAt(5)=='1'){
							System.out.println("Operacion Auth");
							
							//LEEMOS EL FICHERO BINES.TXT
							boolean encontrado = false;
							for(int i=0; i<a2.length && encontrado == false;i++){

								if(a2[i].charAt(0) == 'T'){

									String chu[] = a2[i].split("=");
									if(chu[0].equals("Tarjeta")){
										System.out.println("El numero de tarjeta es: "+chu[1]);
										numero = chu[1];
										encontrado = true;
									}
								}
							}

								try{
									cin = new FileReader("Bines.txt");
									lee = new BufferedReader(cin);
								} catch(IOException e) {
									System.out.println("ERROR. Debe existir el fichero Bines.txt");
								}

								while ((fich = lee.readLine()) != null && salir == false){
									fichero = fichero + fich;	
									if(fich.charAt(0)==numero.charAt(0)){

										churro = fich.split("#");
										System.out.println("Por tanto, el navegador que usare es: "+churro[1]);
										System.out.println("");
										naveg = churro[1];
										salir = true;

										//transformo cadena anadiendo el procesador al que se enviara
										//cad = <STX>1#Titular=13#afaef=1#as=2<ETX>12
										
										String vect[] = cad.split("<ETX>"); 
										cad_toProcesador = vect[0] + "&Procesador="+naveg+"&<ETX>"+vect[1];
										
									}
								}

								

								if(salir && churro!=null) envio_gate = true;
								else envio_gate = false;

						}	
								
						else if (cad.charAt(5) == '2') {
							System.out.println("Operacion Status");
							naveg = (a2[1].split("="))[1];
							String divide[] = naveg.split("<");
							naveg = divide[0];
									
						}
						else if (cad.charAt(5) == '3') {
							System.out.println("Operacion FL");
							naveg = (a2[1].split("="))[1];
							String divide[] = naveg.split("<");
							naveg = divide[0];
						}
						else if (cad.charAt(5) == '4') {
							System.out.println("Operacion UL");
							naveg = (a2[1].split("="))[1];
							String divide[] = naveg.split("<");
							naveg = divide[0];	
						}
						else {

							System.out.println("Me ha llegado la cadena mal");
						}

						if(envio_gate){ //si tengo navegador
									
							boolean no_errores = false;		
							try {
								cin = new FileReader("Procesadores.txt");
								lee = new BufferedReader(cin);
							} catch(IOException e) {
									System.out.println("ERROR. Debe existir el fichero Procesadores.txt");
								}
							boolean salir2=false;
							while ((fich = lee.readLine()) != null && salir2 == false){
							
								String linea[] = fich.split("#");
								//System.out.println("linea es: "+linea[0]);
								//System.out.println("naveg: "+naveg);
								if(linea[0].equals(naveg)){
									
									if(linea.length==3){
										IP = linea[1];
										PORT = linea[2];
										no_errores = true;
									}
								}
							}
							
							if(no_errores) {
								
								//Envio a Procesador peticion
								Socket GateProc = new Socket(IP,Integer.parseInt(PORT)); /* *** CAMBIAR *** */ 

								cad_toProcesador= cad_toProcesador.substring(5);
								String v3[] = cad_toProcesador.split("<ETX>");
								String paraLRC2 = v3[0];
								int LRC_gate_proc = calculaLRC(paraLRC2);
								cad_toProcesador = "<STX>"+paraLRC2+"<ETX>"+LRC_gate_proc;

								cad_toProcesador = cad_toProcesador + "\r\n";
								escribeSocket(GateProc, cad_toProcesador);
								System.out.println("He enviado a Procesadores la cadena: "+cad_toProcesador);
										
								//Recibo de Procesador
								String leyendo = "";
								leyendo = leeSocket(GateProc, leyendo);
								//System.out.println("\n Recibo de Procesador: "+leyendo);
								
								v2= leyendo.split("<ETX>");
								sLRC_HTTP = v2[1];
								cad2=leyendo.substring(5);
								v = cad2.split("<ETX>");
								paraLRC = v[0];
								System.out.println("Recibo la cadena: "+paraLRC);
								LRC_HTTP = Integer.parseInt(sLRC_HTTP);
								LRC_gate = calculaLRC(paraLRC);
								System.out.println("LRC de la cadena: "+LRC_gate);
								if(LRC_gate==LRC_HTTP) {
									System.out.println("Los LRC entre Procesador -> Gateway coinciden: cadena recibida con exito\n");
								}
								else {
									System.out.println("Los LRC entre Procesador -> Gateway no coinciden: error al pasar el mensaje\n");
								}
								System.out.println("");

							
								//leyendo=leyendo+"\r\n";
								// LRC_gate_proc = calculaLRC(leyendo);
								//leyendo = "<STX>"+leyendo+"<ETX>"+LRC_gate_proc;
								//leyendo = leyendo + "\r\n";

								dev = leyendo;
							}
							else{ //si no existe el procesador (no se mete en el condicional para poner IP y PUERTO)

								//Enviare a HttpServer UN ERROR
								String error2 ="";
								if(cad.charAt(5)=='1')
									error2="error/bines";
								else {
									error2="error/proc";
								}
								String error ="error/proc\r\n";

								int LRC_gate_proc = calculaLRC(error2);
								error2 = "<STX>"+error2+"<ETX>"+LRC_gate_proc;
								error2 = error2 + "\r\n";

								dev = error2;
							}
						}
						else{	
							String error2 ="";
							if(cad.charAt(5)=='1')
								error2="error/bines";
							else {
								error2="error/proc";
							}
							System.out.println("El formato del fichero Bines.txt es incorrecto. Debe contener navegadores para todos los numeros de tarjeta y cada uno debe asociarlo a un procesador que exista. Modificalo");
							//dev = error2 + "\r\n";
							int LRC_gate_proc = calculaLRC(error2);
							error2 = "<STX>"+error2+"<ETX>"+LRC_gate_proc;
							error2 = error2 + "\r\n";
						}

						
						escribeSocket(ServerGate, dev); 
						System.out.println("He enviado a HTTPServer la cadena: "+dev);				

						ServerGate.close();
						oye++;
						System.out.println("===============");
					}

					else { //si tengo que mostrar el listado de los procesadores

						System.out.println("Hay que mostrar el listado");
						String IP ="";
						String PORT = "";
						String para_proc = "li";
						String para_ser = "";
						FileReader cin = null;
						BufferedReader lee = null;
						String fich = null;
						String fichero = "";

						try {
								cin = new FileReader("Procesadores.txt");
								lee = new BufferedReader(cin);
						} catch(IOException e) {
							System.out.println("ERROR. Debe existir el fichero Procesadores.txt");
						}
						boolean salir2=false;
						while ((fich = lee.readLine()) != null && salir2 == false){
							System.out.println("LINEA: "+fich);

							String linea[] = fich.split("#");
														
							if(linea.length==3){
								IP = linea[1];
								PORT = linea[2];
								para_proc = para_proc + "/" + IP + "/" + PORT+"\r\n";

								Socket GateProc = new Socket(IP,Integer.parseInt(PORT)); /* *** CAMBIAR *** */ 
								escribeSocket(GateProc, para_proc);
								System.out.println("Escribo a Procesador: "+para_proc);

								String pami = "";

								
								pami = leeSocket(GateProc, para_proc);
								System.out.println("Recibo de Procesador: "+pami);
								String [] v = pami.split("/");

								para_ser = para_ser + "Procesador: "+v[0]+"<br>Estado: "+v[1]+"<br>Floor: "+v[2]+"<br>Upper: "+v[3]+"<br><br>";
							}
						}
						para_ser = para_ser + "\r\n";
						escribeSocket(ServerGate, para_ser);
						System.out.println("Le mando la cadena a HTTPServer");

					}

					


				}
			}
			catch(Exception e){		
				System.out.println("Error: " + e.toString());			
			}    
		}
	}
