'use strict';

angular.module('subutai.plugins.hadoop.controller', [])
    .controller('HadoopCtrl', HadoopCtrl)
	.directive('colSelectHadoopContainers', colSelectHadoopContainers)
	.directive('checkboxListDropdown', checkboxListDropdown);

HadoopCtrl.$inject = ['hadoopSrv', 'SweetAlert', 'DTOptionsBuilder', 'DTColumnDefBuilder'];

function HadoopCtrl(hadoopSrv, SweetAlert, DTOptionsBuilder, DTColumnDefBuilder) {
    var vm = this;
	vm.activeTab = 'install';
	vm.welcome = true;
	vm.hadoopInstall = {};
	vm.environments = null;
	vm.clusters = [];
	vm.currentClusterName = "";
	vm.currentEnvironment = {};
	vm.nameNode = {};
	vm.jobTracker = {};
	vm.secNameNode = {};

	//functions
	vm.createHadoop = createHadoop;
	vm.showContainers = showContainers;
	vm.addContainer = addContainer;
	vm.getClustersInfo = getClustersInfo;
	vm.changeClusterScaling = changeClusterScaling;
	vm.deleteCluster = deleteCluster;
	vm.addNode = addNode;
	vm.startNode = startNode;
	vm.stopNode = stopNode;

	setDefaultValues();

    function getEnvironments() {
        hadoopSrv.getEnvironments().success(function (data) {
            vm.environments = [];
            for (var i = 0; i < data.length; ++i) {
                var envPushed = false;
                for (var j = 0; j < data[i].containers.length; ++j) {
                    if (data[i].containers[j].templateName == "hadoop") {
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
                vm.nameNode = vm.containers[0];
                vm.jobTracker = vm.containers[0];
                vm.secNameNode = vm.containers[0];
                vm.hadoopInstall.slaves = [];
            }
        });
    }
    getEnvironments();

	function getClusters(reset) {
	    LOADING_SCREEN();
		hadoopSrv.getClusters().success(function (data) {
		    LOADING_SCREEN("none");
            vm.clusters = data;
            if (vm.clusters !== undefined && vm.clusters.length !== 0 && vm.clusters !== "") {
                if (reset) {
                    console.log ("wat");
                    vm.currentClusterName = vm.clusters[0];
                    getClustersInfo();
                }
            }
		});
	}
	getClusters(true);

	function getClustersInfo() {
		LOADING_SCREEN();
		hadoopSrv.getClusters(vm.currentClusterName).success(function (data) {
            vm.currentCluster = data;
			LOADING_SCREEN('none');;
		});
	}

	function changeClusterScaling(scale) {
		if(vm.currentCluster.clusterName === undefined) return;
		try {
			hadoopSrv.changeClusterScaling(vm.currentCluster.clusterName, scale);
		} catch(e) {}
	}

	function addNode() {
		if(vm.currentCluster.clusterName === undefined) return;
		SweetAlert.swal("Success!", "Node adding is in progress.", "success");
		hadoopSrv.addNode(vm.currentCluster.clusterName).success(function (data) {
			SweetAlert.swal(
				"Success!",
				"Node has been added to cluster " + vm.currentCluster.clusterName + ".",
				"success"
			);
			getClustersInfo();
		});
	}

	function startNode(node, nodeType) {
		if(vm.currentCluster.clusterName === undefined) return;
		node.status = 'STARTING';
		hadoopSrv.startNode(vm.currentCluster.clusterName, nodeType).success(function (data) {
			SweetAlert.swal("Success!", "Your cluster nodes have been started successfully. LOG: " + data.replace(/\\n/g, ' ').substring (0, 40), "success");
			node.status = 'RUNNING';
			getClustersInfo();
		}).error(function (error) {
			SweetAlert.swal("ERROR!", 'Failed to start cluster error: ' + error.replace(/\\n/g, ' '), "error");
			node.status = 'ERROR';
		});
	}

	function stopNode(node, nodeType) {
		if(vm.currentCluster.clusterName === undefined) return;
		node.status = 'STOPPING';
		hadoopSrv.stopNode(vm.currentCluster.clusterName, nodeType).success(function (data) {
			SweetAlert.swal("Success!", "Your cluster nodes have stopped successfully. LOG: " + data.replace(/\\n/g, ' ').substring (0, 40), "success");
			getClustersInfo();
			node.status = 'STOPPED';
		}).error(function (error) {
			SweetAlert.swal("ERROR!", 'Failed to stop cluster error: ' + error.replace(/\\n/g, ' '), "error");
			node.status = 'ERROR';
		});
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
				hadoopSrv.deleteCluster(vm.currentCluster.clusterName).success(function (data) {
					SweetAlert.swal("Deleted!", "Cluster has been deleted.", "success");
					vm.currentCluster = {};
					vm.currentClusterName = "";
					setTimeout (function() {
                        getClusters(true);
                    }, 1000);
				});
			}
		});
	}

	function createHadoop() {
		SweetAlert.swal("Success!", "Hadoop cluster is being created.", "success");
		vm.activeTab = 'manage';
		LOADING_SCREEN();
		vm.hadoopInstall.environmentId = vm.currentEnvironment.id;
		vm.hadoopInstall.nameNode = vm.nameNode.id;
		vm.hadoopInstall.jobTracker = vm.jobTracker.id;
		vm.hadoopInstall.secNameNode = vm.secNameNode.id;
		hadoopSrv.createHadoop(JSON.stringify(vm.hadoopInstall)).success(function (data) {
			SweetAlert.swal("Success!", "Hadoop cluster creation message:" + data.replace(/\\n/g, ' '), "success");
			getClusters(true);
            setDefaultValues();
			LOADING_SCREEN ("none");
		}).error(function (error) {
			SweetAlert.swal("ERROR!", 'Hadoop cluster creation error: ' + error.replace(/\\n/g, ' '), "error");
			LOADING_SCREEN ("none");
		});
	}

	function showContainers(environmentId) {
		vm.containers = vm.currentEnvironment.containers;
        vm.nameNode = vm.containers[0];
        vm.jobTracker = vm.containers[0];
        vm.secNameNode = vm.containers[0];
        vm.hadoopInstall.slaves = [];
	}

	function addContainer(containerId) {
		if(vm.hadoopInstall.slaves.indexOf(containerId) > -1) {
			vm.hadoopInstall.slaves.splice(vm.hadoopInstall.slaves.indexOf(containerId), 1);
		} else {
			vm.hadoopInstall.slaves.push(containerId);
		}
	}	

	vm.dtOptions = DTOptionsBuilder
		.newOptions()
		.withOption('order', [[2, "asc" ]])
		.withOption('stateSave', true)
		.withPaginationType('full_numbers');
	vm.dtColumnDefs = [
		DTColumnDefBuilder.newColumnDef(0).notSortable(),
		DTColumnDefBuilder.newColumnDef(1),
		DTColumnDefBuilder.newColumnDef(2),
		DTColumnDefBuilder.newColumnDef(3).notSortable()
	];

	function setDefaultValues() {
		vm.hadoopInstall = {};
		vm.hadoopInstall.domainName = 'intra.lan';
		vm.hadoopInstall.replicationFactor = 1;
		vm.hadoopInstall.slaves = [];
	}	


	vm.info = {};
	hadoopSrv.getPluginInfo().success (function (data) {
		vm.info = data;
	});
}

function colSelectHadoopContainers() {
	return {
		restrict: 'E',
		templateUrl: 'plugins/hadoop/directives/col-select/col-select-containers.html'
	}
};

function checkboxListDropdown() {
	return {
		restrict: 'A',
		link: function(scope, element, attr) {
			$(".b-form-input_dropdown").click(function () {
				$(this).toggleClass("is-active");
			});

			$(".b-form-input-dropdown-list").click(function(e) {
				e.stopPropagation();
			});
		}
	}
};

