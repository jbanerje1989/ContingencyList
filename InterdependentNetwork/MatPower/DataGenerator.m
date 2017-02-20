% Data Generator

clc
clear

tic 

% mpc = loadcase('case5');
mpc = loadcase('case14');
%mpc = loadcase('case30');
% mpc = loadcase('case57');
% mpc = loadcase('case118');
% mpc = loadcase('case300');
% mpc = loadcase('case1354pegase');
% mpc = loadcase('case1888rte');
% mpc = loadcase('case13659pegase');

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

%%
% Generates sequence of lines (looak at DEV and eventually IL)
Bus = mpc.bus;
Brnch = mpc.branch;
c1 = 1;
for j=1:length(Bus)
    for k=1:length(Brnch)
        if Bus(j,1)==Brnch(k,1)
            A(c1,1) = Brnch(k,1);
            A(c1,2) = Brnch(k,2);
            c1 = c1 + 1;
        end
    end
end
for j=1:length(A)
    test = 0;
    for k=1:length(Bus)
        if Bus(k)==A(j,2)
            test = test + 1;
        end
    end
    if test==0
        A(j,1) = 0;
        A(j,2) = 0;
    end
end
A(all(A==0,2),:)=[];
D = unique(A);
for j=1:length(D)
    for k=1:length(A)
        if A(k,1)==D(j)
            E(k,1) = j;
        end
        if A(k,2)==D(j)
            E(k,2) = j;
        end
    end
end
F = E;
c1 = length(E) + 1;
for j=1:c1-1
    F(c1+j-1,1) = E(j,2);
    F(c1+j-1,2) = E(j,1);
end
col = [1,2];
G = sortrows(F,col);
DEV = zeros(length(G),3);
for j=1:length(G)
    DEV(j,1) = j;
    DEV(j,2) = G(j,1);
    DEV(j,3) = G(j,2);
end
%%

%%
% Generates line parameters (Look at H and line_param)
Z = zeros(length(A),3);
B = zeros(length(A),3);
for j=1:length(A)
    for k=1:length(Brnch)
        if(A(j,1)==Brnch(k,1) && A(j,2)==Brnch(k,2))
            Z(j,1) = E(j,1);
            Z(j,2) = E(j,2);
            Z(j,3) = Brnch(k,3) + 1i*Brnch(k,4);
            B(j,1) = E(j,1);
            B(j,2) = E(j,2);
            B(j,3) = Brnch(k,5)/2;
        end
    end
end
line_param = [ real(Z(:,3)) imag(Z(:,3)) 2*B(:,3) ];
H = E;
for j=1:length(E)
    if(H(j,1)>H(j,2))
        c = H(j,1);
        H(j,1) = H(j,2);
        H(j,2) = c;
    end
end
I = sortrows(H,col);
for j=1:length(E)
    I(j,3) = j;
end
J = DEV;
for j=1:length(DEV)
    for k=1:length(E)
        if(J(j,3)==I(k,1) && J(j,2)==I(k,2))
            J(j,4) = I(k,3);
        end
    end
end
c = 1;
for j=1:length(J)
    if(J(j,4)==0)
        J(j,4) = c;
        c = c + 1;
    end
end
col1 = [1,2,4,3];
LOC = J(:,col1);
%%

%%
% Calculate line currents (look at DEV and IL)
[m,garbage] = size(V);
[len,garbage1] = size(line_param);
y = zeros(len,1);
for j=1:len
    y(j) = 1/(line_param(j,1) + 1i*line_param(j,2));
end
b = 1i*(line_param(:,3))/2;
[l,garbage2] = size(LOC); % l is the number of measurements  
% Changes arbitrary bus numbers to 1,2,3,4,...
Bus_org = 1:length(Bus);
for j=1:l
    for k=1:length(Bus_org)
        if(Bus_org(k)==LOC(j,2))
            LOC(j,2) = k;
        end
        if(Bus_org(k)==LOC(j,4))
            LOC(j,4) = k;
        end
    end
end
% Computes currents for m number of measurements
IL = zeros(m,l);
for j=1:m
    for k=1:l
        IL(j,k) = y(LOC(k,3))*(V(j,LOC(k,2))-V(j,LOC(k,4))) + b(LOC(k,3))*V(j,LOC(k,2));
    end
end
%%

toc