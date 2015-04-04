# backup-system
Sdis

Para compilar o projecto usamos o seguinte comando:
  - find . -name "*.java" | xargs javac

Para iniciar o sistema de recuperação deve seguir o seguinte comando:
 - java Client.Peer _Multicast Control Channel_ _port_ _Muticast Backup Channel_ _port_ _Muticast Restore Channel_ _port_ _dir_ _space_
    - Os primeiros são os pares ip/port dos três canais a ser usados
    - -dir- é o diretorio onde o peer inicial vai guardar os seus dados, chunks e ficheiros.
    - -space- é o espaço que esse utilizador vai ter no sistema

Para testar o protocolo desenvolvido pode ser usado podem ser usados 4 comandos:
- BACKUP _File Path_ _Replication Degree_
  - -File Path- é o sítio onde se encontra o ficheiro a ser replicado;
  - -Replication Degree- é o numero de replicações mínimas requiridas pelo utilizador que vai replicar o ficheiro.
  
- Restore _Ficheiro_
  - Nome do ficheiro a replicar.
  
- DELETE _Ficheiro_
  - Nome do ficheiro a apagar.
  
- RECLAIM _Space_
  - Space in bytes required to delete.
