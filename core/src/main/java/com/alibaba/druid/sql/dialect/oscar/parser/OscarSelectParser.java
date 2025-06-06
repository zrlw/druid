/*
 * Copyright 1999-2017 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.druid.sql.dialect.oscar.parser;

import com.alibaba.druid.sql.ast.*;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.oscar.ast.stmt.OscarFunctionTableSource;
import com.alibaba.druid.sql.dialect.oscar.ast.stmt.OscarSelectQueryBlock;
import com.alibaba.druid.sql.parser.*;
import com.alibaba.druid.util.FnvHash;

import java.util.List;

public class OscarSelectParser extends SQLSelectParser {
    public OscarSelectParser(SQLExprParser exprParser) {
        super(exprParser);
    }

    public OscarSelectParser(SQLExprParser exprParser, SQLSelectListCache selectListCache) {
        super(exprParser, selectListCache);
    }

    public OscarSelectParser(String sql) {
        this(new OscarExprParser(sql));
    }

    protected OscarExprParser createExprParser() {
        return new OscarExprParser(lexer);
    }

    @Override
    public SQLSelectQuery query(SQLObject parent, boolean acceptUnion) {
        if (lexer.token() == Token.VALUES) {
            return valuesQuery(acceptUnion);
        }

        if (lexer.token() == Token.LPAREN) {
            lexer.nextToken();

            SQLSelectQuery select = query();
            if (select instanceof SQLSelectQueryBlock) {
                ((SQLSelectQueryBlock) select).setParenthesized(true);
            }
            accept(Token.RPAREN);
            select.setParenthesized(true);

            return queryRest(select, acceptUnion);
        }

        OscarSelectQueryBlock queryBlock = new OscarSelectQueryBlock();

        if (lexer.hasComment() && lexer.isKeepComments()) {
            queryBlock.addBeforeComment(lexer.readAndResetComments());
        }

        if (lexer.token() == Token.SELECT) {
            lexer.nextToken();

            if (lexer.token() == Token.COMMENT) {
                lexer.nextToken();
            }

            if (lexer.token() == Token.DISTINCT) {
                queryBlock.setDistionOption(SQLSetQuantifier.DISTINCT);
                lexer.nextToken();

                if (lexer.token() == Token.ON) {
                    lexer.nextToken();

                    for (;;) {
                        SQLExpr expr = this.createExprParser().expr();
                        queryBlock.getDistinctOn().add(expr);
                        if (lexer.token() == Token.COMMA) {
                            lexer.nextToken();
                            continue;
                        } else {
                            break;
                        }
                    }
                }
            } else if (lexer.token() == Token.ALL) {
                queryBlock.setDistionOption(SQLSetQuantifier.ALL);
                lexer.nextToken();
            }

            if (lexer.token() == Token.TOP) {
                SQLTop top = this.createExprParser().parseTop();
                queryBlock.setTop(top);
            }

            parseSelectList(queryBlock);

            if (lexer.token() == Token.INTO) {
                lexer.nextToken();

                if (lexer.token() == Token.LOCAL) {
                    lexer.nextToken();
                    queryBlock.setIntoOptionLocal(OscarSelectQueryBlock.IntoOptionLocal.LOCAL);
                } else if (lexer.token() == Token.GLOBAL) {
                    lexer.nextToken();
                    queryBlock.setIntoOptionLocal(OscarSelectQueryBlock.IntoOptionLocal.GLOBAL);
                }

                if (lexer.token() == Token.TEMPORARY) {
                    lexer.nextToken();
                    queryBlock.setIntoOptionTemp(OscarSelectQueryBlock.IntoOptionTemp.TEMPORARY);
                } else if (lexer.token() == Token.TEMP) {
                    lexer.nextToken();
                    queryBlock.setIntoOptionTemp(OscarSelectQueryBlock.IntoOptionTemp.TEMP);
                } else

                if (lexer.token() == Token.TABLE) {
                    lexer.nextToken();
                }

                SQLExpr name = this.createExprParser().name();

                queryBlock.setInto(new SQLExprTableSource(name));
            }
        }

        parseFrom(queryBlock);

        parseWhere(queryBlock);

        parseGroupBy(queryBlock);

        if (lexer.token() == Token.WINDOW) {
            this.parseWindow(queryBlock);
        }

        queryBlock.setOrderBy(this.createExprParser().parseOrderBy());

        for (;;) {
            if (lexer.token() == Token.LIMIT) {
                SQLLimit limit = getOrInitLimit(queryBlock);

                lexer.nextToken();
                if (lexer.token() == Token.ALL) {
                    limit.setRowCount(new SQLIdentifierExpr("ALL"));
                    lexer.nextToken();
                } else {
                    limit.setRowCount(expr());
                }

                queryBlock.setLimit(limit);
            } else if (lexer.token() == Token.OFFSET) {
                SQLLimit limit = getOrInitLimit(queryBlock);
                lexer.nextToken();
                SQLExpr offset = expr();
                limit.setOffset(offset);

                if (lexer.token() == Token.ROW || lexer.token() == Token.ROWS) {
                    lexer.nextToken();
                }
            } else {
                break;
            }
        }

        if (lexer.token() == Token.FETCH) {
            lexer.nextToken();
            OscarSelectQueryBlock.FetchClause fetch = new OscarSelectQueryBlock.FetchClause();

            if (lexer.token() == Token.FIRST) {
                fetch.setOption(OscarSelectQueryBlock.FetchClause.Option.FIRST);
            } else if (lexer.token() == Token.NEXT) {
                fetch.setOption(OscarSelectQueryBlock.FetchClause.Option.NEXT);
            } else {
                throw new ParserException("expect 'FIRST' or 'NEXT'. " + lexer.info());
            }

            SQLExpr count = expr();
            fetch.setCount(count);

            if (lexer.token() == Token.ROW || lexer.token() == Token.ROWS) {
                lexer.nextToken();
            } else {
                throw new ParserException("expect 'ROW' or 'ROWS'. " + lexer.info());
            }

            if (lexer.token() == Token.ONLY) {
                lexer.nextToken();
            } else {
                throw new ParserException("expect 'ONLY'. " + lexer.info());
            }

            queryBlock.setFetch(fetch);
        }

        if (lexer.token() == Token.FOR) {
            lexer.nextToken();

            OscarSelectQueryBlock.ForClause forClause = new OscarSelectQueryBlock.ForClause();

            if (lexer.token() == Token.UPDATE) {
                forClause.setOption(OscarSelectQueryBlock.ForClause.Option.UPDATE);
                lexer.nextToken();
            } else if (lexer.token() == Token.SHARE) {
                forClause.setOption(OscarSelectQueryBlock.ForClause.Option.SHARE);
                lexer.nextToken();
            } else {
                throw new ParserException("expect 'FIRST' or 'NEXT'. " + lexer.info());
            }

            if (lexer.token() == Token.OF) {
                for (;;) {
                    SQLExpr expr = this.createExprParser().expr();
                    forClause.getOf().add(expr);
                    if (lexer.token() == Token.COMMA) {
                        lexer.nextToken();
                        continue;
                    } else {
                        break;
                    }
                }
            }

            if (lexer.token() == Token.NOWAIT) {
                lexer.nextToken();
                forClause.setNoWait(true);
            } else if (lexer.identifierEquals(FnvHash.Constants.SKIP)) {
                lexer.nextToken();
                acceptIdentifier("LOCKED");
                forClause.setSkipLocked(true);
            }

            queryBlock.setForClause(forClause);
        }

        return queryRest(queryBlock, acceptUnion);
    }

    private SQLLimit getOrInitLimit(SQLSelectQueryBlock queryBlock) {
        SQLLimit limit = queryBlock.getLimit();
        if (limit == null) {
            limit = new SQLLimit();
            queryBlock.setLimit(limit);
        }
        return limit;
    }

    public SQLTableSource parseTableSourceRest(SQLTableSource tableSource) {
        if (lexer.token() == Token.AS && tableSource instanceof SQLExprTableSource) {
            lexer.nextToken();

            String alias = null;
            if (lexer.token() == Token.IDENTIFIER) {
                alias = lexer.stringVal();
                lexer.nextToken();
            }

            if (lexer.token() == Token.LPAREN) {
                SQLExprTableSource exprTableSource = (SQLExprTableSource) tableSource;

                OscarFunctionTableSource functionTableSource = new OscarFunctionTableSource(exprTableSource.getExpr());
                if (alias != null) {
                    functionTableSource.setAlias(alias);
                }

                lexer.nextToken();
                parserParameters(functionTableSource.getParameters());
                accept(Token.RPAREN);

                return super.parseTableSourceRest(functionTableSource);
            }
            if (alias != null) {
                tableSource.setAlias(alias);
                return super.parseTableSourceRest(tableSource);
            }
        }

        return super.parseTableSourceRest(tableSource);
    }

    private void parserParameters(List<SQLParameter> parameters) {
        for (;;) {
            SQLParameter parameter = new SQLParameter();

            parameter.setName(this.exprParser.name());
            parameter.setDataType(this.exprParser.parseDataType());

            parameters.add(parameter);
            if (lexer.token() == Token.COMMA || lexer.token() == Token.SEMI) {
                lexer.nextToken();
            }

            if (lexer.token() != Token.BEGIN && lexer.token() != Token.RPAREN) {
                continue;
            }

            break;
        }
    }
}
