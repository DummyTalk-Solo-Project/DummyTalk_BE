FROM docker.elastic.co/elasticsearch/elasticsearch:8.6.0

RUN bin/elasticsearch-plugin install --batch analysis-nori