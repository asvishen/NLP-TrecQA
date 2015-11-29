package edu.asu.cse.nlp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RuntimeExec {
	public StreamWrapper getStreamWrapper(InputStream is, String type) {
		return new StreamWrapper(is, type);
	}

	private class StreamWrapper extends Thread {
		InputStream is = null;
		String type = null;
		String message = null;

		public String getMessage() {
			return message;
		}

		StreamWrapper(InputStream is, String type) {
			this.is = is;
			this.type = type;
		}

		public void run() {
			try {
				BufferedReader br = new BufferedReader(
						new InputStreamReader(is));
				StringBuffer buffer = new StringBuffer();
				String line = null;
				while ((line = br.readLine()) != null) {
					buffer.append(line);
					buffer.append("\n");
					// .append("\n");
				}
				message = buffer.toString();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	// this is where the action is
	public String getType(String myQuery) throws InterruptedException, IOException{
		Runtime rt = Runtime.getRuntime();
		RuntimeExec rte = new RuntimeExec();
		StreamWrapper error, output;

		try {

			//String myquery = "What_is_the_capital_of_India?";
			//String[] arr = { "What is the capital of India?"};
			myQuery = myQuery.trim();
			myQuery = myQuery.replace(" ", "_");
			//System.out.println("query is ---- " + myQuery);
			String s = "C:/Users/Avani/Anaconda2/python C:/Users/Avani/git/Factoid-Question-Answering/answer_classifier.py --query "
					+ myQuery;

			//System.out.println(s);

			Process proc = rt.exec(s);

			error = rte.getStreamWrapper(proc.getErrorStream(), "ERROR");
			output = rte.getStreamWrapper(proc.getInputStream(), "OUTPUT");
			int exitVal = 0;

			//			error.start();
			output.start();
			//			error.join(3000);
			output.join();
			//exitVal = proc.waitFor();
			//System.out.println("Output: " + output.message);

			String answerType = output.message;
			//System.out.println(answerType);
			return answerType;

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}