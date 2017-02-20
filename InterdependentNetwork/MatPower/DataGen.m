% -------------------------------------------
% Assumes each bus has a unique number between [1,numBuses] with bus data
% having entries in increasing order of bus number
function DataArray = DataGen(caseFile)
% Data Generator
mpc = loadcase(caseFile);

%%
% Generates Voltages
l = 30; % Number of samples
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
end
%%

%%Read the Bus data from case file and load DataArray
Bus = mpc.bus;
Branch = mpc.branch;
DataArray(1) = size(Bus,1); %stores num of buses
DataArray(2) = size(V,1);%stores num of time samples
DataArray(3) = size(Branch,1);
index = 4;
%loads data from case file
for i = 1:size(Bus,1)
    DataArray(index) = Bus(i,1); % bus number
    DataArray(index + 1) = Bus(i,2); % bus type 
    DataArray(index + 2) = Bus(i,3); % bus Real Load
    DataArray(index + 3) = Bus(i,4); % bus Imaginary Load
    DataArray(index + 4) = Bus(i,10); % base Kv
    for j = 1 : size(V,1)
        DataArray(index + 4 + 2 *j - 1) = real(V(j,i)); %volage on bus real at time j
        DataArray(index + 4 + 2 * j) = imag(V(j,1)); %volatge on bus imag at time j
    end
    
    index  = index + 5 + 2 * size(V,1);
end

% read the brach data
for i = 1:size(Branch,1)
    DataArray(index) = Branch(i,1);
    DataArray(index + 1) = Branch(i,2);
    index  = index + 2;
end
end
