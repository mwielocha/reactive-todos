CREATE IF NOT EXISTS KEYSPACE todos WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };
USE todos;
CREATE TABLE todos (userId bigint, id timeuuid, done boolean, name text, PRIMARY KEY (userId, id));
CREATE TRIGGER notification_trigger ON todos USING 'com.cyberdolphins.triggers.TodoNotificationTrigger';
insert into todos (userid, id, name, done) values (1, 1d0753c0-0b8a-11e5-a0b3-3ef9c498367e, 'Hallo from Cassandra!', false);