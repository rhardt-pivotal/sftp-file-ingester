package io.pivotal.fe.streamdemo.sftpfileingester;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

@Log
@SpringBootApplication
@EnableBinding(Processor.class)
@Controller
@EnableConfigurationProperties(SftpFileIngesterProperties.class)
public class SftpFileIngesterApplication {

	public static void main(String[] args) {
		SpringApplication.run(SftpFileIngesterApplication.class, args);
	}

	@Autowired
	private MessageChannel output;

	@Autowired
	private SftpFileIngesterProperties sftpProperties;

	@StreamListener(Processor.INPUT)
	public void handle(String file) {
		log.warning("PROCESSING: "+file);
		try{
			InputStream is = getSessionFactory().getSession().getClientInstance().get(file);
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			//iterate and process 1-to-many lines here.  
			//when you have an object you can send downstream,
			//send it via output.send
			while ((line = br.readLine()) != null) {
				output.send(MessageBuilder.withPayload(line).build());
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			output.send(MessageBuilder.withPayload(ex).build());
		}

	}


	//is this necessary for SCDF to recognize this as 'deployed'??
	@RequestMapping("/")
	public ResponseEntity<String> index(){
		return ResponseEntity.ok("Nobody here but us chickens");
	}


	private DefaultSftpSessionFactory getSessionFactory() {
		DefaultSftpSessionFactory sessionFactory = new DefaultSftpSessionFactory();
		sessionFactory.setHost(sftpProperties.getHost());
		sessionFactory.setPort(sftpProperties.getPort());
		sessionFactory.setUser(sftpProperties.getUsername());
		sessionFactory.setPassword(sftpProperties.getPassword());
		sessionFactory.setAllowUnknownKeys(sftpProperties.isAllowUnknownKeys());

		return sessionFactory;
	}



}

