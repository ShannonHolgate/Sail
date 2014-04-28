call mongo test2 --eval "db.investmenthistorys.drop()"
call mongo test2 --eval "db.investments.drop()"
call mongo test2 --eval "db.users.drop()"

call mongoimport --collection investmenthistorys --db test2 --file InvestmentHistorys.json
call mongoimport --collection investments --db test2 --file Investments.json
call mongoimport --collection users --db test2 --file User.json
