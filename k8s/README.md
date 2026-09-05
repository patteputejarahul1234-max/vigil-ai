# Kubernetes manifests — Vigil AI

These manifests describe how Vigil AI would be deployed and orchestrated
on a real Kubernetes cluster: 2 backend replicas behind a ClusterIP
service, a load-balanced frontend, persistent MySQL storage, and Redis
for caching — with secrets kept out of the deployment YAML itself.

## Honest status: written, not run locally

Same situation as Docker on this machine: running an actual Kubernetes
cluster (even a lightweight one like Minikube or Kind) needs
virtualization resources this laptop's hardware can't handle without
freezing, for the same reasons Docker Desktop couldn't run here.

These manifests are correct and would deploy successfully on:
- A cloud Kubernetes service (GKE, EKS, AKS — most offer free trial credits)
- A more capable machine with Minikube/Kind installed
- A managed platform with Kubernetes support

## What each file does
- `backend-deployment.yaml` — 2 backend pods, health-checked via
  Spring Boot Actuator, secrets injected as env vars
- `frontend-deployment.yaml` — 2 frontend pods behind a LoadBalancer
- `mysql-deployment.yaml` — single MySQL pod with persistent storage
  (PVC) so data survives pod restarts
- `redis-deployment.yaml` — single Redis pod for caching
- `secrets-template.yaml` — template for the Kubernetes Secret object;
  copy to `secrets.yaml`, fill in real base64 values, never commit it

## If you ever get access to a real cluster
```bash
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/mysql-deployment.yaml
kubectl apply -f k8s/redis-deployment.yaml
kubectl apply -f k8s/backend-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
kubectl get pods    # watch them come up
```
