# backup-system
Sdis

Para compilar o projecto usamos o seguinte comando:
  - find . -name "*.java" | xargs javac

Para iniciar o sistema de recuperação deve seguir o seguinte comando:
 - java Client.Peer -Multicast Control Channel- <port> <Muticast Backup Channel> <port> <Muticast Restore Channel> <port> <dir> <space>
    - Os primeiros são os pares ip/port dos três canais a ser usados
    - <dir> é o diretorio onde o peer inicial vai guardar os seus dados, chunks e ficheiros.
    - <space> é o espaço que esse utilizador vai ter no sistema

Para testar o protocolo desenvolvido pode ser usado podem ser usados 4 comandos:
- BACKUP <File Path> <Replication Degree>
  - <File Path> é o sítio onde se encontra o ficheiro a ser replicado;
  - <Replication Degree> é o numero de replicações mínimas requiridas pelo utilizador que vai replicar o ficheiro.
  
- Restore <Ficheiro> 
  - Nome do ficheiro a replicar.
  
- DELETE <Ficheiro>
  - Nome do ficheiro a apagar.
  
- RECLAIM <Space>
  - Space in bytes required to delete.
