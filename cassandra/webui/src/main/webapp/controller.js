'use strict';

angular.module('subutai.plugins.cassandra.controller', [])
    .controller('CassandraCtrl', CassandraCtrl)
	.directive('colSelectContainers', colSelectContainers)
	.directive('colSelectSeeds', colSelectSeeds);

CassandraCtrl.$inject = ['cassandraSrv', 'SweetAlert', 'DTOptionsBuilder', 'DTColumnDefBuilder', '$timeout'];
function CassandraCtrl(cassandraSrv, SweetAlert, DTOptionsBuilder, DTColumnDefBuilder, $timeout) {
    var vm = this;
	vm.activeTab = 'install';
	vm.clusterNodesChkbx = false;
	vm.cassandraInstall = {};
	vm.environments = [];
	vm.currentEnvironment = {};
	vm.containers = [];
	vm.seeds = [];

	vm.clusters = [];
	vm.currentCluster = {};
	vm.currentClusterName = "";
	vm.nodes2Action = [];

	//functions
	vm.addContainer = addContainer;
	vm.addSeed = addSeed;
	vm.createCassandra = createCassandra;

	vm.getClustersInfo = getClustersInfo;
	vm.changeClusterScaling = changeClusterScaling;
	vm.deleteCluster = deleteCluster;
	vm.addNode = addNode;
	vm.deleteNode = deleteNode;
	vm.pushNode = pushNode;
	vm.startNodes = startNodes;
	vm.stopNodes = stopNodes;
	vm.pushAll = pushAll;

	setDefaultValues();
	cassandraSrv.getEnvironments().success(function (data) {
	    vm.environments = [];
	    for (var i = 0; i < data.length; ++i) {
	        var envPushed = false;
	        for (var j = 0; j < data[i].containers.length; ++j) {
	            if (data[i].containers[j].templateName == "cassandra") {
                    if (!envPushed) {
                        envPushed = true;
                        vm.environments.push (angular.copy (data[i]));
                        vm.environments[vm.environments.length - 1].containers = [];
                    }
                    vm.environments[vm.environments.length - 1].containers.push (data[i].containers[j]);
	            }
	        }
	    }
	    console.log (vm.environments);
	    vm.currentEnvironment = vm.environments[0];
	    vm.containers = vm.currentEnvironment.containers;
		if (vm.environments !== undefined && vm.environments.length !== 0 || vm.environments !== "")
		{
		    vm.currentEnvironment = vm.environments[0];
		    vm.seeds = [];

		}
	});

	function getClusters(reset) {
	    LOADING_SCREEN();
		cassandraSrv.getClusters().success(function (data) {
		    LOADING_SCREEN("none");
			vm.clusters = data;
			if (vm.clusters !== undefined && vm.clusters.length !== 0 && vm.clusters !== "") {
			    if (reset) {
			        vm.currentClusterName = vm.clusters[0];
			        getClustersInfo();
			    }
			}
		});
	}
	getClusters(true);

	vm.dtOptions = DTOptionsBuilder
		.newOptions()
		.withOption('order', [[2, "asc" ]])
		.withOption('stateSave', true)
		.withPaginationType('full_numbers');
	vm.dtColumnDefs = [
		DTColumnDefBuilder.newColumnDef(0).notSortable(),
		DTColumnDefBuilder.newColumnDef(1),
		DTColumnDefBuilder.newColumnDef(2),
		DTColumnDefBuilder.newColumnDef(3),
		DTColumnDefBuilder.newColumnDef(4).notSortable(),
		DTColumnDefBuilder.newColumnDef(5).notSortable()
	];

	/*function reloadTableData() {
		vm.refreshTable = $timeout(function myFunction() {
			if(typeof(vm.dtInstance.reloadData) == 'function') {
				vm.dtInstance.reloadData(null, false);
			}
			vm.refreshTable = $timeout(reloadTableData, 3000);
		}, 3000);
	};
	reloadTableData();*/

	function getClustersInfo() {
		LOADING_SCREEN();
		cassandraSrv.getClusters(vm.currentClusterName).success(function (data) {
			vm.currentCluster = data;
			LOADING_SCREEN('none');
		});
	}

	function startNodes() {
		if(vm.nodes2Action.length == 0) return;
		if(vm.currentCluster.name === undefined) return;
		SweetAlert.swal({
			title : 'Success!',
			text : 'Your request is in progress. You will be notified shortly.',
			timer: VARS_TOOLTIP_TIMEOUT,
			showConfirmButton: false
		});
		LOADING_SCREEN();
		cassandraSrv.startNodes(vm.currentCluster.name, JSON.stringify(vm.nodes2Action)).success(function (data) {
			SweetAlert.swal("Success!", "Your cluster nodes started successfully.", "success");
			console.log ("getting cluster info");
			getClustersInfo(vm.currentCluster.name);
		}).error(function (error) {
			SweetAlert.swal("ERROR!", 'Cluster start error: ' + error.replace(/\\n/g, ' '), "error");
			LOADING_SCREEN("none");
		});
	}

	function stopNodes() {
		if(vm.nodes2Action.length == 0) return;
		if(vm.currentCluster.name === undefined) return;
		SweetAlert.swal({
			title : 'Success!',
			text : 'Your request is in progress. You will be notified shortly.',
			timer: VARS_TOOLTIP_TIMEOUT,
			showConfirmButton: false
		});
		LOADING_SCREEN();
		cassandraSrv.stopNodes(vm.currentCluster.name, JSON.stringify(vm.nodes2Action)).success(function (data) {
			SweetAlert.swal("Success!", "Your cluster nodes stoped successfully.", "success");
			console.log ("getting cluster info");
			getClustersInfo(vm.currentCluster.name);
		}).error(function (error) {
			SweetAlert.swal("ERROR!", 'Cluster stop error: ' + error.replace(/\\n/g, ' '), "error");
			LOADING_SCREEN("none");
		});
	}

	function pushNode(id) {
		if(vm.nodes2Action.indexOf(id) >= 0) {
			vm.nodes2Action.splice(vm.nodes2Action.indexOf(id), 1);
			vm.clusterNodesChkbx = false;
		} else {
			vm.nodes2Action.push(id);
			if (vm.nodes2Action.length === vm.currentCluster.containers.length) {
                vm.clusterNodesChkbx = true;
			}
		}
	}

	function addNode() {
		if(vm.currentCluster.name === undefined) return;
		SweetAlert.swal("Success!", "Adding node action started.", "success");
		cassandraSrv.addNode(vm.currentCluster.name).success(function (data) {
			SweetAlert.swal(
				"Success!",
				"Node has been added on cluster " + vm.currentCluster.name + ".",
				"success"
			);
			getClusters(false);
			vm.activeTab = 'manage';
			setDefaultValues();
			getClustersInfo(vm.currentCluster.name);
		});
	}

	function deleteNode(nodeId) {
		if(vm.currentCluster.name === undefined) return;
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
				cassandraSrv.deleteNode(vm.currentCluster.name, nodeId).success(function (data) {
					SweetAlert.swal("Deleted!", "Node has been deleted.", "success");
					vm.currentCluster = {};
				});
			}
		});
	}

	function deleteCluster() {
		if(vm.currentCluster.name === undefined) return;
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
				cassandraSrv.deleteCluster(vm.currentCluster.name).success(function (data) {
					SweetAlert.swal("Deleted!", "Cluster has been deleted.", "success");
					vm.currentCluster = {};
					vm.currentClusterName = "";
					getClusters(true);
				});
			}
		});
	}

	function createCassandra() {
		SweetAlert.swal("Success!", "Your Cassandra cluster started creating.", "success");
		vm.activeTab = "manage";
		vm.cassandraInstall.environmentId = vm.currentEnvironment.id;
		LOADING_SCREEN();
		cassandraSrv.createCassandra(JSON.stringify(vm.cassandraInstall)).success(function (data) {
			SweetAlert.swal("Success!", "Your Cassandra cluster successfully created.", "success");
			getClusters(true);
		}).error(function (error) {
			SweetAlert.swal("ERROR!", 'Cassandra cluster create error: ' + error.replace(/\\n/g, ' '), "error");
		});
	}

	function changeClusterScaling(scale) {
		if(vm.currentCluster.name === undefined) return;
		try {
			cassandraSrv.changeClusterScaling(vm.currentCluster.name, scale);
		} catch(e) {}
	}

	function addContainer(containerId) {
		if(vm.cassandraInstall.containers.indexOf(containerId) > -1) {
			vm.cassandraInstall.containers.splice(vm.cassandraInstall.containers.indexOf(containerId), 1);
		} else {
			vm.cassandraInstall.containers.push(containerId);
		}
		vm.seeds = angular.copy(vm.cassandraInstall.containers);
	}

	function addSeed(seedId) {
		if(vm.cassandraInstall.seeds.indexOf(seedId) > -1) {
			vm.cassandraInstall.seeds.splice(vm.cassandraInstall.seeds.indexOf(seedId), 1);
		} else {
			vm.cassandraInstall.seeds.push(seedId);
		}
	}
	
	function setDefaultValues() {
		vm.cassandraInstall.domainName = 'intra.lan';
		vm.cassandraInstall.dataDir = '/var/lib/cassandra/data';
		vm.cassandraInstall.commitDir = '/var/lib/cassandra/commitlog';
		vm.cassandraInstall.cacheDir = '/var/lib/cassandra/saved_caches';
		vm.cassandraInstall.containers = [];
		vm.cassandraInstall.seeds = [];
	}


	function pushAll() {
		if (vm.currentCluster.name !== undefined) {
			if (vm.nodes2Action.length === vm.currentCluster.containers.length) {
				vm.nodes2Action = [];
			}
			else {
				for (var i = 0; i < vm.currentCluster.containers.length; ++i) {
					vm.nodes2Action.push (vm.currentCluster.containers[i]);
				}
			}
			console.log (vm.nodes2Action);
		}
	}


	vm.info = {};
	cassandraSrv.getPluginInfo().success (function (data) {
		vm.info = data;
	});
}

function colSelectContainers() {
	return {
		restrict: 'E',
		templateUrl: 'plugins/cassandra/directives/col-select/col-select-containers.html'
	}
};

function colSelectSeeds() {
	return {
		restrict: 'E',
		templateUrl: 'plugins/cassandra/directives/col-select/col-select-seeds.html'
	}
};

