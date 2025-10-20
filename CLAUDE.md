
# Project: Database Import Tool for set up information
## Tech stack 
- Database supported : PostGres
- Langague : java 
- build : Maven based pom xml . Keep it simple . We want to generate artifact and support unit test.
- Libraries : Keep the code simple . 
    Use native code whereever possible and choose external libraries where we need to write lot of boiler plate code.   

## Resources
- csv files : in resources folder 

## configuration
- Database connection configuration should be present in resources 

## Folder structure
- Should exist like any standard java project with `src` and `test` folder

## Conventions
- Table names match CSV filenames (employee.csv → employee table)
- First row contains column names
- Next row contains data 
- Use parameterized queries for SQL injection protection