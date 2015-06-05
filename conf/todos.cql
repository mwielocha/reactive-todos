CREATE KEYSPACE todos WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };
CREATE TABLE todos.Todos (userId bigint, id timeuuid, done boolean, name text, PRIMARY KEY (userId, id));
CREATE TRIGGER notification_trigger ON todos.Todos USING 'com.cyberdolphins.triggers.TodoNotificationTrigger';