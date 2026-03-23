FROM docker.elastic.co/elasticsearch/elasticsearch:8.11.1

RUN bin/elasticsearch-plugin remove analysis-nori || true
RUN bin/elasticsearch-plugin install --batch analysis-nori