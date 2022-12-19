package it.gov.pagopa.swclient.mil.session.dao;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class SessionService {
 
	 private final ReactiveValueCommands<String, Session> commands;

	 public SessionService(ReactiveRedisDataSource ds) {
		 commands = ds.value(Session.class);
	 }

	 public Uni<Void> set(String sessionId, Session session) {
		 return commands.set(sessionId, session);
	 }
	 
	 public Uni<Session> get(String sessionId) {
		 return commands.get(sessionId);
	 }
	 
	 public Uni<Session> getdel(String sessionId) {
		 return commands.getdel(sessionId);
	 }
	 	
}
