# Event Grid Documentation

### Create Event Grid
### Create resource group
```az group create --name myResourceGroup --location eastus```

### Create storage account
```
az storage account create \
--name mystorageaccount \
--resource-group myResourceGroup \
--location eastus \
--sku Standard_LRS \
--kind StorageV2
```

### Get storage account key
```
az storage account keys list \
--account-name mystorageaccount \
--resource-group myResourceGroup
```

### Create container
```
az storage container create \
--name uploads \
--account-name mystorageaccount \
--account-key <your-account-key>
```

### Create Service Bus namespace (NOT USED)
```
az servicebus namespace create \
--name myservicebusnamespace \
--resource-group myResourceGroup \
--location eastus
```

### Create queue (NOT USED)
```
az servicebus queue create \
--name file-upload-notifications \
--namespace-name myservicebusnamespace \
--resource-group myResourceGroup
```

### Create Event Grid topic
```
az eventgrid topic create \
--name myeventgridtopic \
--resource-group myResourceGroup \
--location eastus
```

### Create webhook subscription
```
az eventgrid event-subscription create \
--name blobsubscription \
--source-resource-id /subscriptions/{subscription-id}/resourceGroups/myResourceGroup/providers/Microsoft.Storage/storageAccounts/mystorageaccount \
--endpoint https://yourdomain.com/api/webhook/eventgrid \
--included-event-types Microsoft.Storage.BlobCreated Microsoft.Storage.BlobDeleted
```