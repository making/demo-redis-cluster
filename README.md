
```
curl "http://localhost:8080?key=foo" -H "Content-Type: text/plain" -d Hello
curl "http://localhost:8080?key=foo"
```

or go to http://localhost:8080/swagger-ui.html

## How to deploy to Tanzu Application Platform

## Deploy Redis Cluster on Kubernetes

### Using Bitnami Helm Chart for Redis Cluster

```yaml
kubectl create ns demo
PASSWORD=$(uuidgen | tr '[A-Z]' '[a-z]')
cat <<EOF > secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: redis
type: servicebinding.io/redis
stringData:
  type: redis
  cluster.nodes: redis-cluster-0.redis-cluster-headless:6379,redis-cluster-1.redis-cluster-headless:6379,redis-cluster-2.redis-cluster-headless:6379,redis-cluster-3.redis-cluster-headless:6379,redis-cluster-4.redis-cluster-headless:6379,redis-cluster-5.redis-cluster-headless:6379
  password: ${PASSWORD}
  redis-password: ${PASSWORD}  
---
apiVersion: services.apps.tanzu.vmware.com/v1alpha1
kind: ResourceClaim
metadata:
  name: redis
spec:
  ref:
    apiVersion: v1
    kind: Secret
    name: redis
EOF

helm template redis-cluster bitnami/redis-cluster --set existingSecret=redis -n demo | kubectl apply -f - -f secret.yaml -n demo --dry-run=client
```

### Using VMware Tanzu GemFire for Redis App

#### Deploy VMware Tanzu GemFire for Kubernetes

```
TANZUNET_USERNAME=...
TANZUNET_PASSWORD=...

imgpkg pull -b registry.tanzu.vmware.com/tanzu-gemfire-for-kubernetes/gemfire-for-kubernetes-carvel-bundle:2.1.0 -o workspace/tg4k8s_carvel_bundle

cd workspace/tg4k8s_carvel_bundle

kubectl create ns gemfire-system --dry-run=client -oyaml > namespace.yaml
kubectl create secret docker-registry image-pull-secret --namespace=gemfire-system --docker-server=registry.tanzu.vmware.com --docker-username=${TANZUNET_USERNAME} --docker-password=${TANZUNET_PASSWORD} --dry-run=client -oyaml > image-pull-secret.yaml
ytt -f operator.yaml -f certificates.yaml -f values.yaml -f functions.lib.yml -f namespace.yaml -f image-pull-secret.yaml | kbld -f- | kapp deploy -a gemfire-operator -f- -c -y
```

### Create a VMware Tanzu GemFire for Redis Apps Cluster

```yaml
cat <<EOF > redis.yaml
apiVersion: gemfire.vmware.com/v1
kind: GemFireCluster
metadata:
  name: redis
spec:
  image: registry.tanzu.vmware.com/pivotal-gemfire/vmware-gemfire:9.15.1
  antiAffinityPolicy: None
  security:
    tls: {}
  metrics:
    emission: Default
  locators:
    replicas: 2
    resources:
      requests:
        memory: 1Gi
  servers:
    replicas: 2
    resources:
      requests:
        memory: 1Gi
    libraries:
      - name: gemfire-for-redis-apps
        container:
          image: registry.tanzu.vmware.com/tanzu-gemfire-for-redis-apps/gemfire-for-redis-apps:1.0.1
          path: "/gemfire-for-redis-apps/*"
          imagePullSecretRef:
            name: image-pull-secret
    overrides:
      jvmOptions: ["-Dgemfire-for-redis-enabled=true"]
EOF
```

```yaml
cat <<EOF > secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: redis
type: servicebinding.io/redis
stringData:
  type: redis
  cluster.nodes: redis-server-0.redis-server.demo.svc.cluster.local:6379,redis-server-1.redis-server.demo.svc.cluster.local:6379
---
apiVersion: services.apps.tanzu.vmware.com/v1alpha1
kind: ResourceClaim
metadata:
  name: redis
spec:
  ref:
    apiVersion: v1
    kind: Secret
    name: redis
EOF
```

```
kubectl create ns demo
kubectl create secret -n demo docker-registry image-pull-secret --docker-server=registry.tanzu.vmware.com --docker-username=${TANZUNET_USERNAME} --docker-password=${TANZUNET_PASSWORD}
kubectl apply -f redis.yaml -f secret.yaml -n demo 
```

## Create a workload for Tanzu Application Platform

```
tanzu apps workload apply demo-redis \
  --app demo-redis \
  --git-repo https://github.com/making/demo-redis-cluster \
  --git-branch main \
  --type web \
  --service-ref=redis=services.apps.tanzu.vmware.com/v1alpha1:ResourceClaim:redis \
  --build-env BP_JVM_VERSION=17 \
  --label apps.tanzu.vmware.com/has-tests=true \
  --label apis.apps.tanzu.vmware.com/register-api=true \
  --param-yaml api_descriptor='{"description":"Demo Redis","location":{"path":"/v3/api-docs"},"owner":"demo","system":"dev","type":"openapi"}' \
  --annotation autoscaling.knative.dev/minScale=1 \
  -n demo \
  -y
```