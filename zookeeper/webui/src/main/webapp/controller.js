'use strict';

angular.module('subutai.plugins.zookeeper.controller', [])
    .controller('ZookeeperCtrl', ZookeeperCtrl)
	.directive('colSelectZookeeperNodes', colSelectZookeeperNodes);

ZookeeperCtrl.$inject = ['$scope', 'zookeeperSrv', 'SweetAlert', 'DTOptionsBuilder', 'DTColumnDefBuilder', 'ngDialog'];

function ZookeeperCtrl($scope, zookeeperSrv, SweetAlert, DTOptionsBuilder, DTColumnDefBuilder, ngDialog) {
    var vm = this;
	vm.activeTab = 'install';
	vm.installType = false;
	vm.containersFieldName = 'id';
	vm.zookeeperInstall = {};
	vm.hadoopFullInfo = {};
	vm.clusters = [];
	vm.environments = null;
	vm.hadoopClusters = null;
	vm.currentClusterNodes = [];
	vm.currentCluster = {};
	vm.containers = [];
	vm.nodes2Action = [];

	//functions
	vm.setInstallType = setInstallType;	
	vm.getClustersInfo = getClustersInfo;	
	vm.getEnvironmentNodes = getEnvironmentNodes;
	vm.getHadoopClusterNodes = getHadoopClusterNodes;
	vm.addContainer = addContainer;	
	vm.createZookeeper = createZookeeper;
	vm.deleteNode = deleteNode;;
	vm.addNode = addNode;
	vm.addNodeForm = addNodeForm;
	vm.deleteCluster = deleteCluster;
	vm.pushNode = pushNode;
	vm.pushAll = pushAll;
	vm.changeClusterScaling = changeClusterScaling;
	vm.startNodes = startNodes;
	vm.stopNodes = stopNodes;
	vm.resetInstallType = resetInstallType;

	function setInstallType(installType) {
		vm.installType = installType;
		if(vm.installType == 'hadoop') {
			vm.containersFieldName = 'uuid';
		}
	}

	function resetInstallType() {
		vm.installType = false;
		setDefaultValues();
	}

	zookeeperSrv.getEnvironments().success(function(data){
		vm.environments = [];
        for (var i = 0; i < data.length; ++i) {
            var envPushed = false;
            for (var j = 0; j < data[i].containers.length; ++j) {
                if (data[i].containers[j].templateName == "zookeeper") {
                    if (!envPushed) {
                        envPushed = true;
                        vm.environments.push (angular.copy (data[i]));
                        vm.environments[vm.environments.length - 1].containers = [];
                    }
                    vm.environments[vm.environments.length - 1].containers.push (data[i].containers[j]);
                }
            }
        }
        if (vm.environments !== undefined && vm.environments !== "" && vm.environments.length !== 0) {
            vm.currentEnvironment = vm.environments[0];
            vm.currentClusterNodes = vm.currentEnvironment.containers;
            vm.zookeeperInstall.nodes = [];
        }
	}).error(function(error){
		SweetAlert.swal("ERROR!", 'No environments were found! ERROR: ' + error.replace(/\\n/g, ' '), "error");
	});

	zookeeperSrv.getHadoopClusters().success(function(data){
		vm.hadoopClusters = data;
	}).error(function(error){
		SweetAlert.swal("ERROR!", 'No Hadoop clusters was found! ERROR: ' + error.replace(/\\n/g, ' '), "error");
	});
	setDefaultValues();

	function getClusters() {
		vm.clusters = [];
		LOADING_SCREEN();
		zookeeperSrv.getClusters().success(function (data) {
			vm.clusters = data;
			LOADING_SCREEN("none");
		});
	}
	getClusters();

	vm.dtOptions = DTOptionsBuilder
		.newOptions()
		.withOption('order', [[1, "asc" ]])
		.withOption('stateSave', true)
		.withPaginationType('full_numbers');

	vm.dtColumnDefs = [
		DTColumnDefBuilder.newColumnDef(0).notSortable(),
		DTColumnDefBuilder.newColumnDef(1),
		DTColumnDefBuilder.newColumnDef(2),
		DTColumnDefBuilder.newColumnDef(3),
		DTColumnDefBuilder.newColumnDef(4).notSortable(),
	];

	function getClustersInfo(selectedCluster) {
		LOADING_SCREEN();
		vm.currentCluster = {};
		vm.nodes2Action = [];
		zookeeperSrv.getClusters(selectedCluster).success(function (data) {
			vm.currentCluster = data;
			LOADING_SCREEN('none');
		});
	}

	function getEnvironmentNodes(selectedCluster) {
		vm.currentClusterNodes = vm.currentEnvironment.containers;
		vm.zookeeperInstall.nodes = [];
	}

	function getHadoopClusterNodes(selectedCluster) {
		LOADING_SCREEN();
		vm.currentClusterNodes = [];
		zookeeperSrv.getHadoopClusters(selectedCluster).success(function (data) {
			vm.hadoopFullInfo = data;
			vm.currentClusterNodes = data.dataNodes;
			var tempArray = [];

			var nameNodeFound = false;
			var jobTrackerFound = false;
			var secondaryNameNodeFound = false;
			for(var i = 0; i < vm.currentClusterNodes.length; i++) {
				var node = vm.currentClusterNodes[i];
				if(node.hostname === data.nameNode.hostname) nameNodeFound = true;
				if(node.hostname === data.jobTracker.hostname) jobTrackerFound = true;
				if(node.hostname === data.secondaryNameNode.hostname) secondaryNameNodeFound = true;
			}
			if(!nameNodeFound) {
				tempArray.push(data.nameNode);
			}
			if(!jobTrackerFound) {
				if(tempArray[0].hostname != data.jobTracker.hostname) {
					tempArray.push(data.jobTracker);
				}
			}
			if(!secondaryNameNodeFound) {
				var checker = 0;
				for(var i = 0; i < tempArray.length; i++) {
					if(tempArray[i].hostname != data.secondaryNameNode.hostname) {
						checker++;
					}
				}
				if(checker == tempArray.length) {
					tempArray.push(data.secondaryNameNode);
				}
			}
			vm.currentClusterNodes = vm.currentClusterNodes.concat(tempArray);

			LOADING_SCREEN('none');
		});
	}

	function createZookeeper() {
		if(vm.zookeeperInstall.clusterName === undefined || vm.zookeeperInstall.clusterName.length == 0) return;
		SweetAlert.swal("Success!", "Zookeeper cluster start creating.", "success");
		LOADING_SCREEN();
		if(vm.installType == 'hadoop') {
			vm.zookeeperInstall.environmentId = vm.hadoopFullInfo.environmentId;
		}
		zookeeperSrv.createZookeeper(vm.zookeeperInstall, vm.installType).success(function (data) {
			SweetAlert.swal("Success!", "Zookeeper cluster successfully created.", "success");
			LOADING_SCREEN("none");
			getClusters();
		}).error(function (error) {
			SweetAlert.swal("ERROR!", 'Zookeeper cluster create error: ' + error, "error");
			LOADING_SCREEN("none");
			getClusters();
		});
		setDefaultValues();
		vm.activeTab = 'manage';
	}

	function deleteCluster() {
		if(vm.currentCluster.clusterName === undefined) return;
		SweetAlert.swal({
			title: "Are you sure?",
			text: "Your will not be able to recover this cluster!",
			type: "warning",
			showCancelButton: true,
			confirmButtonColor: "#ff3f3c",
			confirmButtonText: "Delete",
			cancelButtonText: "Cancel",
			closeOnConfirm: false,
			closeOnCancel: true,
			showLoaderOnConfirm: true
		},
		function (isConfirm) {
			if (isConfirm) {
				zookeeperSrv.deleteCluster(vm.currentCluster.clusterName).success(function (data) {
					SweetAlert.swal("Deleted!", "Cluster has been deleted.", "success");
					vm.currentCluster = {};
					getClusters();
				}).error(function(error){
					SweetAlert.swal("ERROR!", 'Delete cluster error: ' + error, "error");
				});
			}
		});
	}

	function deleteNode(nodeId) {
		if(vm.currentCluster.clusterName === undefined) return;
		SweetAlert.swal({
			title: "Are you sure?",
			text: "Your will not be able to recover this node!",
			type: "warning",
			showCancelButton: true,
			confirmButtonColor: "#ff3f3c",
			confirmButtonText: "Delete",
			cancelButtonText: "Cancel",
			closeOnConfirm: false,
			closeOnCancel: true,
			showLoaderOnConfirm: true
		},
		function (isConfirm) {
			if (isConfirm) {
				zookeeperSrv.deleteNode(vm.currentCluster.clusterName, nodeId).success(function (data) {
					SweetAlert.swal("Deleted!", "Node has been deleted.", "success");
					getClustersInfo(vm.currentCluster.clusterName);
				}).error(function(error){
					SweetAlert.swal("ERROR!", 'Delete cluster error: ' + error, "error");
				});
			}
		});
	}

	function addContainer(containerId) {
		if(vm.zookeeperInstall.nodes.indexOf(containerId) > -1) {
			vm.zookeeperInstall.nodes.splice(vm.zookeeperInstall.nodes.indexOf(containerId), 1);
		} else {
			vm.zookeeperInstall.nodes.push(containerId);
		}
	}

	function setDefaultValues() {
		vm.zookeeperInstall = {};
		vm.zookeeperInstall.nodes = [];
		vm.installType = false;
		vm.currentClusterNodes = [];
		vm.containersFieldName = 'id';
		vm.hadoopFullInfo = {};
		vm.currentCluster = {};
	}

	function pushNode(id) {
		if(vm.nodes2Action.indexOf(id) >= 0) {
			vm.nodes2Action.splice(vm.nodes2Action.indexOf(id), 1);
		} else {
			vm.nodes2Action.push(id);
		}
	}

	function pushAll() {
		if (vm.currentCluster.nodes !== undefined) {
			if (vm.nodes2Action.length === vm.currentCluster.nodes.length) {
				vm.nodes2Action = [];
			}
			else {
				for (var i = 0; i < vm.currentCluster.nodes.length; ++i) {
					vm.nodes2Action.push(vm.currentCluster.nodes[i].hostname);
				}
			}
		}
	}

	function changeClusterScaling (val) {
		zookeeperSrv.changeClusterScaling (vm.currentCluster.clusterName, val);
	}

	function startNodes() {
		if(vm.nodes2Action.length == 0) return;
		if(vm.currentCluster.clusterName === undefined) return;
		SweetAlert.swal({
			title : 'Success!',
			text : 'Your request is in progress. You will be notified shortly.',
			timer: VARS_TOOLTIP_TIMEOUT,
			showConfirmButton: false
		});
		zookeeperSrv.startNodes(vm.currentCluster.clusterName, JSON.stringify(vm.nodes2Action)).success(function (data) {
			SweetAlert.swal("Success!", "Nodes started successfully.", "success");
			getClustersInfo(vm.currentCluster.clusterName);
		}).error(function (error) {
			SweetAlert.swal("ERROR!", 'Cluster start error: ' + error.replace(/\\n/g, ' '), "error");
		});
	}

	function stopNodes() {
		if(vm.nodes2Action.length == 0) return;
		if(vm.currentCluster.clusterName === undefined) return;
		SweetAlert.swal({
			title : 'Success!',
			text : 'Your request is in progress. You will be notified shortly.',
			timer: VARS_TOOLTIP_TIMEOUT,
			showConfirmButton: false
		});
		zookeeperSrv.stopNodes(vm.currentCluster.clusterName, JSON.stringify(vm.nodes2Action)).success(function (data) {
			SweetAlert.swal("Success!", "Nodes stoped successfully.", "success");
			getClustersInfo(vm.currentCluster.clusterName);
		}).error(function (error) {
			SweetAlert.swal("ERROR!", 'Cluster stop error: ' + error.replace(/\\n/g, ' '), "error");
		});
	}

	function addNodeForm() {
		if(vm.currentCluster.clusterName === undefined) return;
		zookeeperSrv.getAvailableNodes(vm.currentCluster.clusterName).success(function (data) {
			vm.availableNodes = data;
		});
		ngDialog.open({
			template: 'plugins/zookeeper/partials/addNodesForm.html',
			scope: $scope
		});
	}

	function addNode(chosenNode) {
		if(chosenNode === undefined) return;
		if(vm.currentCluster.clusterName === undefined) return;
		SweetAlert.swal("Success!", "Adding node action started.", "success");
		ngDialog.closeAll();
		zookeeperSrv.addNode(vm.currentCluster.clusterName, chosenNode).success(function (data) {
			SweetAlert.swal(
				"Success!",
				"Node has been added on cluster " + vm.currentCluster.clusterName + ".",
				"success"
			);
			getClustersInfo(vm.currentCluster.clusterName);
		}).error(function(error){
			SweetAlert.swal("ERROR!", 'Adding node error: ' + error.replace(/\\n/g, ' '), "error");
		});
	}

	vm.info = {};
    zookeeperSrv.getPluginInfo().success (function (data) {
    	vm.info = data;
    });
}

function colSelectZookeeperNodes() {
	return {
		restrict: 'E',
		templateUrl: 'plugins/zookeeper/directives/col-select/col-select-containers.html'
	}
};

