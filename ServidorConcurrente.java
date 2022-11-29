import java.net.*;

public class ServidorConcurrente {

	/**
	 * @param args
	 */
	@SuppressWarnings("resource")

	public static int actuales = 0;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*
		* Descriptores de socket servidor y de socket con el cliente
		*/
		String puerto="";
		String puerto_escribo="";
		String ip_escribo="";

		try
		{
			
			if (args.length < 4) {
				System.out.println("Debe indicar el puerto de escucha de HttpServer, el puerto de escucha del Gateway, la IP del Gateway y el numero maximo de conexiones simultaneas");
				System.out.println("Ejemplo: java ServidorConcurrente puerto_Http puerto_gate max_clientes");
				
				System.exit (1);
			}
			puerto = args[0];
			puerto_escribo = args[1];
			ip_escribo = args[2];
			int maximo = Integer.parseInt(args[3]);
			ServerSocket skServidor = new ServerSocket(Integer.parseInt(puerto));
		    System.out.println("Escucho el puerto " + puerto);
			
			
			/*
			* Mantenemos la comunicacion con el cliente
			*/	
			System.out.println("Esperando a clientes...");
			for(;;)
			{
				
				
				//Se espera un cliente que quiera conectarse
				Socket skCliente = skServidor.accept(); // Crea objeto

				//***CONTROLAR LOS FAVINCON AQUI***
		        

		        Thread t = new HiloServidor(skCliente, Integer.parseInt(puerto_escribo), maximo, ip_escribo);
		        t.start();
				actuales++;
			}

		}
		catch(Exception e)
		{
			System.out.println("Error: " + e.toString());
			
		}
	}

}
