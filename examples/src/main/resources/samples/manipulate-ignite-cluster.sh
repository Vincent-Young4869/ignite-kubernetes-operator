#!/bin/bash

if [[ $# -eq 0 ]]; then
  echo "Usage: $0 <arg>"
  echo "arg: 'c' or 'create' to apply; 'd' or 'delete' to delete"
  exit 1
fi

case "$1" in
  c|create)
    kubectl apply -f serviceaccount.yaml
    kubectl apply -f ignite-service.yaml
    kubectl apply -f ignite-configmap.yaml
    kubectl apply -f ignite-cluster-statefulset.yaml
    ;;
  d|delete)
    kubectl delete -f ignite-cluster-statefulset.yaml
    kubectl delete -f ignite-configmap.yaml
    kubectl delete -f ignite-service.yaml
    kubectl delete -f serviceaccount.yaml
    ;;
  *)
    echo "Invalid argument: $1"
    echo "Usage: $0 <arg>"
    echo "arg: 'c' or 'create' to apply; 'd' or 'delete' to delete"
    exit 1
    ;;
esac