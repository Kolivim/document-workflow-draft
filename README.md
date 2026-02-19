Для создания контейнера с БД :
docker run -it -d --name doc_wf -e POSTGRES_PASSWORD=docWf -e POSTGRES_USER=docWf -e POSTGRES_DB=doc_workflow  
-p 5432:5432 -d postgres:17.6


# document-workflow

Backend-сервис по работе c документами. 
Документы создаются, переводятся по статусам, по изменениям статуса ведётся история.


Для запуска приложения локально нужно запустить docker-desktop и DBeaver 
для просмотра БД (url, username, password можно взять из docker-compose)


запустить файл docker-compose

запустить приложение

swagger доступен по http://localhost:8080/swagger-ui/index.html#

логи доступны в консоли запущенного приложения

документы создаются по 10(число n) каждую минуту по scheduler

документы передаются на согласование и утверждение каждые 2 минуты по 8(число batchSize) по scheduler
