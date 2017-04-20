% -------------------------------------------
% Assumes each bus has a unique number between [1,numBuses] with bus data
% having entries in increasing order of bus number
function DataArray = DataGen(caseFile)
% Data Generator
mpc = loadcase(caseFile);
l = 1; % Number of samples

Bus = mpc.bus;
Branch = mpc.branch;
Gen = mpc.gen;
DataArray = zeros(3 + size(Bus,1) * 6 + size(Branch,1) * 4, 1);
DataArray(1) = size(Bus,1); %stores num of buses
DataArray(2) = l;%stores num of time samples
DataArray(3) = size(Branch,1);

% Generates Voltages
mpc0 = mpc.bus(:,3:4);
s = 0.001*mpc.bus(:,3);
ss = 0.001*mpc.bus(:,4);
V = zeros(l,length(mpc0));
for j=1:l
    k = 1 + j*5.5556e-04;
    w1 = randn(length(mpc0),1);
    w2 = randn(length(mpc0),1);
    mpc.bus(:,3:4) = k*mpc0(:,:);
    mpc.bus(:,3) = mpc.bus(:,3) + s.*w1;
    mpc.bus(:,4)= mpc.bus(:,4) + ss.*w2;
    results = runopfnoprint(mpc);
    w = results.bus(:,8:9);
    V(j,1:length(mpc0)) = w(:,1).*exp(1i*w(:,2)*pi/180);

%%Read the Bus data from case file and load DataArray
resultGen = results.gen;
index = 4;
%loads data from case file
for i = 1:size(Bus,1)
    DataArray(index) = Bus(i,1); % bus number
    DataArray(index + 1) = Bus(i,2); % bus type 
    DataArray(index + 2) = Bus(i,3); % bus real load value
    DataArray(index + 3) = Bus(i,4); % bus imaginary load value
    DataArray(index + 4) = 0; % generator max real power
    if (~isempty(find(Bus(i,1) == Gen(:,1))))
        % get generator value of the bus
        findVal = find(Bus(i,1) == Gen(:,1));
        for k = 1: size(findVal)
            DataArray(index + 4) = DataArray(index + 4) + 0.8 * Gen(findVal(k),9);
        end
    end
    DataArray(index + 5) = 0; % real power from generator
    if (~isempty(find(Bus(i,1) == resultGen(:,1))))
        % get generator value of the bus
        findVal = find(Bus(i,1) == resultGen(:,1));
        for k = 1: size(findVal)
            DataArray(index + 5) = DataArray(index + 5) + resultGen(findVal(k),2);
        end
    end
    index  = index + 6;
end

% read the brach data
for i = 1:size(Branch,1)
    DataArray(index) = Branch(i,1);
    DataArray(index + 1) = Branch(i,2);
    indexBus1 = find(Bus(:,1) == DataArray(index));
    indexBus2 = find(Bus(:,1) == DataArray(index + 1));
    voltBus1 = V(j,indexBus1);
    voltBus2 = V(j,indexBus2);
    Impedence = complex(Branch(i,3), Branch(i,4));
    current12 = (voltBus1 - voltBus2) / Impedence;
    powerFlow12 = voltBus1 * conj(current12);
    DataArray(index + 2) = real(powerFlow12);
    current21 = (voltBus2 - voltBus1) / Impedence;
    powerFlow21 = voltBus2 * conj(current21);
    DataArray(index + 3) = real(powerFlow21);
    index  = index + 4;
end
end
end

