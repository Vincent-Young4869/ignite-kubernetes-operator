# Helm Charts for Ignite Kubernetes Operator Deployment

## Deployment Steps
1. build the ignite-kubernetes-operator module
   - the latest CRD will be generated under `<project_path>/ignite-kubernetes-operator-api/build/classes/java/main/META-INF/fabric8/`
2. copy (or overwrite) the CRD to helm/crds folder
```bash
cp <project_path>/ignite-kubernetes-operator-api/build/classes/java/main/META-INF/fabric8/igniteresources.com.yyc-v1.yml <project_path>/helm/crds
```
3. create namespace for the ignite kubernetes operator, if not exists, and update the `.Values.namespaceOverride`
4. deploy the operator by helm install
```bash
helm upgrade --install <your_operator_release_name> <project_path>/helm
```
5. clean up all operator related resources (except for namesapce)
```bash
helm uninstall <your_operator_release_name>
```