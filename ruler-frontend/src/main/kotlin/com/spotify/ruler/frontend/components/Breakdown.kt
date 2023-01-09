/*
 * Copyright 2021 Spotify AB
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

package com.spotify.ruler.frontend.components

import com.bnorm.react.RFunction
import com.bnorm.react.RKey
import com.spotify.ruler.frontend.formatSize
import com.spotify.ruler.models.AppComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.FileContainer
import com.spotify.ruler.models.Measurable
import kotlinx.html.id
import kotlinx.js.jso
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLSelectElement
import react.RBuilder
import react.useState
import react.useMemo
import react.StateSetter
import react.dom.button
import react.dom.div
import react.dom.nav
import react.dom.ul
import react.dom.li
import react.dom.option
import react.dom.select
import react.dom.strong
import react.dom.h2
import react.dom.h4
import react.dom.span
import react.table.columns
import react.table.useTable
import react.table.RenderType
import react.table.usePagination
import react.table.useExpanded
import react.table.TableInstance
import react.table.Row
import react.table.Cell
import react.table.TableCell

private val FILE_CONTAINER_COLUMNS = columns<FileContainer> {
    column<String> {
        accessorFunction = { it.name }
        id = "name"
    }

    column<String?> {
        accessorFunction = { it.owner }
        id = "owner"
    }

    column<Long> {
        accessorFunction = { it.downloadSize }
        id = "downloadSize"
    }

    column<Long> {
        accessorFunction = { it.installSize }
        id = "installSize"
    }
}

@RFunction
fun RBuilder.breakdown(components: List<AppComponent>, sizeType: Measurable.SizeType) {
    h4(classes = "mb-3") { +"Breakdown (${components.size} components)" }
    div(classes = "row") {
        containerList(components, sizeType)
    }
}

@RFunction
fun RBuilder.containerList(containers: List<FileContainer>, sizeType: Measurable.SizeType) {
    val memoData = useMemo { containers.toTypedArray() }
    val memoColumns = useMemo { FILE_CONTAINER_COLUMNS }

    val table = useTable<FileContainer>(
        options = jso {
            data = memoData
            columns = memoColumns
            initialState = jso {
                pageSize = 15
            }
        },
        usePagination
    );

    div(classes = "accordion") {
        table.getTableProps()
        table.getTableBodyProps()
        table.page.mapIndexed { index, row ->
            table.prepareRow(row)
            containerListItem(index, row, sizeType, row.original.name)
        }
    }

    if (!table.page.isEmpty()) {
        div (classes = "row") {
            containerPagination(table)
        }
    }
}

@RFunction
@Suppress("UNUSED_PARAMETER")
fun RBuilder.containerPagination(table: TableInstance<FileContainer>) {
    val totalPages = table.pageCount
    val activePage = table.state.pageIndex
    val pageSize = table.state.pageSize
    val canPreviousPage = table.canPreviousPage
    val canNextPage = table.canNextPage

    val pageSizeOptions = listOf<Int>(10, 20, 30, 40, 50, 100, 200)
    val pageItemClass = "page-item"

    div(classes="col") {
        nav {
        ul(classes = "pagination justify-content-center") {
                val previousButtonClasses = if (canPreviousPage) pageItemClass else "$pageItemClass disabled"
                val nextButtonClasses = if (canNextPage) pageItemClass else "$pageItemClass disabled"
            li(classes = previousButtonClasses) {
                button(classes = "page-link") {
                    attrs.onClickFunction = {
                        table.gotoPage(0)
                    }
                    +"<<"
                }
                }

                li(classes = previousButtonClasses) {
                button(classes = "page-link") {
                    attrs.onClickFunction = {
                        table.previousPage()
                    }
                    +"<"
                }
                }

                li(classes = nextButtonClasses) {
                button(classes = "page-link") {
                    attrs.onClickFunction = {
                        table.nextPage()
                    }
                    +">"
                }
                }

                li(classes = nextButtonClasses) {
                button(classes = "page-link") {
                    attrs.onClickFunction = {
                        table.gotoPage(totalPages - 1)
                    }
                    +">>"
                }
                }

                li(classes = "page-item") {
                    span(classes = "page-link") {
                        +"Page "
                        strong {
                            +"${activePage + 1} of ${totalPages}"
                        }
                    }
                }
        }
        }
    }

    div(classes = "col") {
        select(classes = "form-select") {
            attrs.value = "$pageSize"
            attrs.onChangeFunction = { event ->
                table.setPageSize((event.target as HTMLSelectElement).value.toInt())
            }

            pageSizeOptions.forEach { pageSize ->
                option {
                    attrs.value = "$pageSize"
                    +"Show $pageSize"
                }
            }
        }
    }
}

@RFunction
@Suppress("UNUSED_PARAMETER")
fun RBuilder.containerListItem(id: Int, row: Row<FileContainer>, sizeType: Measurable.SizeType, @RKey key: String) {
    val (expanded, setExpanded) = useState(false)

    div(classes = "accordion-item") {
        row.getRowProps()
        containerListItemHeader(id, row.original, sizeType, expanded, setExpanded)
        containerListItemBody(id, row.original, sizeType, expanded)
    }
}

@RFunction
fun RBuilder.containerListItemHeader(id: Int, container: FileContainer, sizeType: Measurable.SizeType, expanded: Boolean, setExpanded: StateSetter<Boolean>) {
    val containsFiles = container.files != null
    h2(classes = "accordion-header") {
        var classes = "accordion-button collapsed"
        if (!containsFiles) {
            classes = "$classes disabled"
        }

        button(classes = classes) {
            attrs.onClickFunction = {
                setExpanded(!expanded)
            }

            attrs["data-bs-toggle"] = "collapse"
            attrs["data-bs-target"] = "#module-$id-body"
            span(classes = "font-monospace text-truncate me-3") { +container.name }
            container.owner?.let { owner -> span(classes = "badge bg-secondary me-3") { +owner } }
            var sizeClasses = "ms-auto text-nowrap"
            if (containsFiles) {
                sizeClasses = "$sizeClasses me-3"
            }
            span(classes = sizeClasses) {
                +formatSize(container, sizeType)
            }
        }
    }
}

@RFunction
fun RBuilder.containerListItemBody(id: Int, container: FileContainer, sizeType: Measurable.SizeType, expanded: Boolean) {
    div(classes = "accordion-collapse collapse") {
        attrs.id = "module-$id-body"
        div(classes = "accordion-body p-0") {
            if (expanded) {
                fileList(container.files ?: emptyList(), sizeType)
            }
        }
    }
}

@RFunction
fun RBuilder.fileList(files: List<AppFile>, sizeType: Measurable.SizeType) {
    div(classes = "list-group list-group-flush") {
        files.forEach { file ->
            fileListItem(file, sizeType, file.name)
        }
    }
}

@RFunction
@Suppress("UNUSED_PARAMETER")
fun RBuilder.fileListItem(file: AppFile, sizeType: Measurable.SizeType, @RKey key: String) {
    div(classes = "list-group-item d-flex border-0") {
        span(classes = "font-monospace text-truncate me-2") { +file.name }
        span(classes = "ms-auto me-custom text-nowrap") {
            +formatSize(file, sizeType)
        }
    }
}
